import zio.Chunk

object CoapService {
  def fromChunk(chunk: Chunk[Byte]): Either[CoapMessageException, CoapMessage] =
    for {
      header <- headerFromChunk(chunk.take(4))
      body   <- bodyFromChunk(chunk.drop(4), header)
    } yield CoapMessage(header, body)

  private def headerFromChunk(chunk: Chunk[Byte]): Either[CoapMessageException, CoapHeader] =
    if (chunk.lengthCompare(4) == 0) for {
      version <- (getVersion _ andThen CoapVersion.apply)(chunk(0))
      msgType <- (getMsgType _ andThen CoapType.apply)(chunk(0))
      tLength <- (getTLength _ andThen CoapTokenLength.apply)(chunk(0))
      prefix  <- (getCPrefix _ andThen CoapCodePrefix.apply)(chunk(1))
      suffix  <- (getCSuffix _ andThen CoapCodeSuffix.apply)(chunk(1))
      id      <- CoapId(getMsgId(chunk(2), chunk(3)))
    } yield CoapHeader(version, msgType, tLength, prefix, suffix, id)
    else Left(InvalidCoapChunkSize(s"${chunk.size} unfit for header"))

  private def bodyFromChunk(chunk: Chunk[Byte], header: CoapHeader): Either[CoapMessageException, CoapBody] = {
      // extract the token and continue with the remainder
      val length = header.tLength.value
      val token  = CoapToken(chunk.take(length))
      val c      = chunk.drop(length)

      // loop remainder for option values until end is reached / payload marker detected
      def grabOptions(
        rem: Chunk[Byte],
        acc: List[Either[CoapMessageException, CoapOption]] = List.empty,
        sum: Int = 0
      ): Either[CoapMessageException, (List[CoapOption], Option[CoapPayload])] = {
        rem.headOption match {
          case Some(b) if (b != 0xFF.toByte) => {
            val delta = (b & 0xF0) >>> 4

          }
          case Some(_) => acc.partitionMap(identity) match {
            case (Nil, rights) => if (rem.tail.nonEmpty) Right(rights, Some(CoapPayload(rem.drop(1))))
                                  else Left(InvalidPayloadMarker)
            case (lefts,    _) => Left(lefts.head)
          }
          case None    => acc.partitionMap(identity) match {
            case (Nil, rights) => Right(rights, None)
            case (lefts,    _) => Left(lefts.head)
          }
        }
      }

     def parseNextOption(chunk: Chunk[Byte], sum: Int): Either[CoapMessageException, CoapOption] = {
       val optionBody = chunk.drop(1)


       if (chunk.headOption.isDefined) for {
         head      <- chunk.headOption.toRight(InvalidCoapChunkSize("Expect"))
         deltaOff  <- getDelta(chunk.head, optionBody)
         lengthOff <- getLength(chunk.head, optionBody, deltaOff._2)
         valueRaw  <- getValue(optionBody, lengthOff._1, lengthOff._2)
         delta     <- CoapOptionDelta(deltaOff._1)
         length    <- CoapOptionLength(lengthOff._1)
         value      = CoapOptionValue(valueRaw)
         number     = CoapOptionNumber(sum + delta.value)
       } yield CoapOption(delta, length, value, number)
       else Left(InvalidCoapChunkSize("Tried to parse "))
     }
  }

  private def getDelta(b: Byte, chunk: Chunk[Byte]): Either[CoapMessageException, (Int, Int)] =
    (b & 0xF0) >>> 4 match {
      case 13 => extractByte(chunk.take(1)).flatMap(i => Right((i + 13, 1)))
      case 14 => merge2Bytes(chunk.take(2)).flatMap(i => Right((i + 269, 2)))
      case 15 => Left(InvalidOptionDelta("15 is a reserved value."))
      case other => Right((other, 0))
    }

  private def getLength(b: Byte, chunk: Chunk[Byte], offset: Int): Either[CoapMessageException, (Int, Int)] =
    (b & 0x0F) match {
      case 13 => extractByte(chunk.drop(offset).take(1)).flatMap(i => Right((i + 13, offset + 1)))
      case 14 => merge2Bytes(chunk.drop(offset).take(2)).flatMap(i => Right((i + 269, offset + 2)))
      case 15 => Left(InvalidOptionLength("15 is a reserved length value."))
      case other => Right(other, offset)
    }

  private def getValue(chunk: Chunk[Byte], length: Int, offset: Int) =
    if (chunk.lengthCompare(length) >= 0) Right(chunk.take(length))
    else Left(InvalidCoapChunkSize("Packet ended prematurely. Failed to extract whole option value."))

  private def extractByte(bytes: Chunk[Byte]): Either[CoapMessageException, Int] =
    if (bytes.lengthCompare(1) == 0) Right(bytes.head.toInt)
    else Left(InvalidCoapChunkSize("Packet ended prematurely. Failed to extract singular byte."))

  private def merge2Bytes(bytes: Chunk[Byte]): Either[CoapMessageException, Int] =
    if (bytes.lengthCompare(2) == 0) Right((bytes(0) << 8) | (bytes(1) & 0xFF))
    else Left(InvalidCoapChunkSize(s"Expected two bytes for merging, received ${bytes.size}"))

  private def getVersion(b: Byte): Int = (b & 0xF0) >>> 6
  private def getMsgType(b: Byte): Int = (b & 0x30) >> 4
  private def getTLength(b: Byte): Int = b & 0x0F
  private def getCPrefix(b: Byte): Int = (b & 0xE0) >>> 5
  private def getCSuffix(b: Byte): Int = b & 0x1F
  private def getMsgId(third: Byte, fourth: Byte) = (third << 8) | (fourth & 0xFF)
}