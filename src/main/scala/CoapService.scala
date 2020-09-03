import zio.Chunk

import scala.annotation.tailrec

object CoapService {

  /**
   * Takes a chunk and attempts to convert it into a CoapMessage.
   * <p>
   * take and drop might return an empty Chunk instead of failing.
   * Therefore error handling has to be done on different layers of the transformation.
   * <p>
   * Error handling is done via short-circuiting since a malformed packet would throw
   * too many and mostly useless errors. Thus, a top-down error search is implemented.
   */
  def fromChunk(chunk: Chunk[Byte]): Either[CoapMessageException, CoapMessage] =
    for {
      header <- headerFromChunk(chunk.take(4))
      body   <- bodyFromChunk(chunk.drop(4), header)
    } yield CoapMessage(header, body)

  /**
   * Attempts to form a CoapHeader when handed a Chunk of size 4.
   * Might fail with header parameter dependent errors.
   */
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

  /**
   * Extracts the token from the body and attempts to recursively read the options from the body.
   * Before each attempt there is a check for a payload marker and end of chunk.
   *
   * @param chunk can be of any size smaller than the maximum datagram size
   *              which should be filtered by the buffer anyway.
   * @param header is required to extract the respective token length.
   * @return Either a CoapBody or an Exception
   */
  private def bodyFromChunk(chunk: Chunk[Byte], header: CoapHeader): Either[CoapMessageException, CoapBody] = {
      // extract the token and continue with the remainder
      val length = header.tLength.value
      val token  = CoapToken(chunk.take(length))
      val c      = chunk.drop(length)

    @tailrec
    def grabOptions(
        rem: Chunk[Byte],
        acc: List[CoapOption] = List.empty,
        num: Int = 0
      ): Either[CoapMessageException, (List[CoapOption], Option[CoapPayload])] = {
        rem.headOption match {
          // recursively iterates over the chunk and builds a list of the options or throws an exception
          case Some(b) if b != 0xFF.toByte => parseNextOption(b, rem, num) match {
            case Right(option) => grabOptions(rem.drop(option.offset.value), option :: acc, num + option.number.value)
            case Left(err)     => Left(err)
          }
          // a payload marker was detected - according to protocol this fails if there is a marker but no load
          case Some(_) => if (rem.tail.nonEmpty) Right(acc, Some(CoapPayload(rem.drop(1))))
                          else Left(InvalidPayloadMarker)
          case None    => Right(acc, None)
        }
    }
    ???
  }

  private def parseNextOption(head: Byte, chunk: Chunk[Byte], num: Int): Either[CoapMessageException, CoapOption] = {
    val optionBody = chunk.drop(1) // TODO: refactor

    for {
      deltaRes  <- getDelta(head, optionBody)
      (deltaRaw, deltaOffset) = deltaRes
      lengthRes <- getLength(head, optionBody, deltaOffset)
      (lengthRaw, lengthOffset) = lengthRes
      valueRaw  <- getValue(optionBody, lengthRaw, deltaOffset + lengthOffset)
      delta     <- CoapOptionDelta(deltaRaw)
      length    <- CoapOptionLength(lengthRaw)
      value      = CoapOptionValue(valueRaw)
      number     = CoapOptionNumber(num + delta.value)
      offset     = CoapOptionOffset(1 + deltaOffset + lengthOffset + lengthRaw)
    } yield CoapOption(delta, length, value, number, offset)
  }


  private def getDelta(b: Byte, chunk: Chunk[Byte]): Either[CoapMessageException, (Int, Int)] =
    (b & 0xF0) >>> 4 match {
      case 13 => extractByte(chunk.take(1)).flatMap(i => Right((i + 13, 1)))
      case 14 => merge2Bytes(chunk.take(2)).flatMap(i => Right((i + 269, 2)))
      case 15 => Left(InvalidOptionDelta("15 is a reserved value."))
      case other => Right((other, 0))
    }

  private def getLength(b: Byte, chunk: Chunk[Byte], offset: Int): Either[CoapMessageException, (Int, Int)] =
    b & 0x0F match {
      case 13 => extractByte(chunk.drop(offset).take(1)).flatMap(i => Right((i + 13, 1)))
      case 14 => merge2Bytes(chunk.drop(offset).take(2)).flatMap(i => Right((i + 269, 2)))
      case 15 => Left(InvalidOptionLength("15 is a reserved length value."))
      case other => Right(other, 0)
    }

  private def getValue(chunk: Chunk[Byte], length: Int, offset: Int): Either[InvalidCoapChunkSize, Chunk[Byte]] = {
    val dropOptionHeader = chunk.drop(offset)
    if (dropOptionHeader.lengthCompare(length) >= 0) Right(dropOptionHeader.take(length))
    else Left(InvalidCoapChunkSize("Packet ended prematurely. Failed to extract whole option value."))
  }

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
  private def getMsgId(third: Byte, fourth: Byte): Int = (third << 8) | (fourth & 0xFF)

}