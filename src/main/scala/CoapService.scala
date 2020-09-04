package root

import zio.Chunk

import scala.annotation.tailrec

object CoapService {

  /**
   * Takes a chunk and attempts to convert it into a CoapMessage.
   * <p>
   * Calls via take and drop might return an empty Chunk instead of failing.
   * Therefore error handling has to be done on different layers of the transformation.
   * <p>
   * Error handling is done via short-circuiting since a malformed packet would throw
   * too many and mostly useless errors. Thus, a top-down error search is implemented.
   */
  def extractFromChunk(chunk: Chunk[Byte]): Either[CoapMessageException, CoapMessage] =
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
    val length    = header.tLength.value
    val remainder = chunk.drop(length)

    def extractToken(chunk: Chunk[Byte]): Either[CoapMessageException, CoapToken] = {
      if (chunk.lengthCompare(length) >= 0) Right(CoapToken(chunk.take(length)))
      else Left(InvalidCoapChunkSize("Packet ended prematurely. Failed to extract promised token."))
    }

    @tailrec
    def grabOptions(
        rem: Chunk[Byte],
        acc: List[CoapOption] = List.empty,
        num: Int = 0
      ): Either[CoapMessageException, (List[CoapOption], Option[CoapPayload])] = {
        rem.headOption match {
          // recursively iterates over the chunk and builds a list of the options or throws an exception
          case Some(b) if b != 0xFF.toByte => parseNextOption(b, rem, num) match {
            case Right(result) => grabOptions(rem.drop(result.offset.value), result :: acc, num + result.number.value)
            case Left(err)     => Left(err)
          }
          // a payload marker was detected - according to protocol this fails if there is a marker but no load
          case Some(_) => if (rem.tail.nonEmpty) Right(acc, Some(CoapPayload(rem.drop(1))))
                          else Left(InvalidPayloadMarker)
          case None    => Right(acc, None)
        }
    }

    for {
      t       <- extractToken(chunk)
      token    = if (t.value.nonEmpty) Some(t) else None
      optsPay <- grabOptions(remainder)
      options  = if (optsPay._1.nonEmpty) Some(optsPay._1) else None
      payload  = optsPay._2
    } yield CoapBody(token, options, payload)
  }

  private def parseNextOption(optionHeader: Byte, chunk: Chunk[Byte], num: Int): Either[CoapMessageException, CoapOption] = {
    // option header is always one byte - empty check happens during parameter extraction
    val optionBody = chunk.drop(1)

    for {
      // extract delta value from header, possibly extend to second and third byte and pass possible offset
      deltaTuple  <- getDelta(optionHeader, optionBody)
      (delta, deltaOffset) = deltaTuple
      // extract length value from header, possible extension which depends on the offset of the delta value, pass offset
      lengthTuple <- getLength(optionHeader, optionBody, deltaOffset)
      (length, lengthOffset) = lengthTuple
      // get the value starting at the position based on the two offsets, ending at that value plus the length value
      value       <- getValue(optionBody, length, deltaOffset + lengthOffset)
      number       = CoapOptionNumber(num + delta.value)
      // offset can be understood as the size of the parameter group
      offset       = CoapOptionOffset(deltaOffset.value + lengthOffset.value + length.value + 1)
    } yield CoapOption(delta, length, value, number, offset)
  }

  private def getDelta(b: Byte, chunk: Chunk[Byte]): Either[CoapMessageException, (CoapOptionDelta, CoapOptionOffset)] =
    (b & 0xF0) >>> 4 match {
      case 13 => for {
          i <- extractByte(chunk.take(1))
          d <- CoapOptionDelta(i + 13)
        } yield (d, CoapOptionOffset(1))
      case 14 => for {
          i <- merge2Bytes(chunk.take(2))
          d <- CoapOptionDelta(i + 269)
        } yield (d, CoapOptionOffset(2))
      case 15 => Left(InvalidOptionDelta("15 is a reserved value."))
      case other if 0 to 12 contains other => for {
          d <- CoapOptionDelta(other)
        } yield (d, CoapOptionOffset(0))
      case e => Left(InvalidOptionDelta(s"Illegal delta value of ${e}. Initial value must be between 0 and 15."))
    }

  private def getLength(
    b: Byte,
    chunk: Chunk[Byte],
    offset: CoapOptionOffset
  ): Either[CoapMessageException, (CoapOptionLength, CoapOptionOffset)] =
    b & 0x0F match {
      case 13 => for {
          i <- extractByte(chunk.drop(offset.value).take(1))
          l <- CoapOptionLength(i + 13)
        } yield (l, CoapOptionOffset(2))
      case 14 => for {
          i <- merge2Bytes(chunk.drop(offset.value).take(2))
          l <- CoapOptionLength(i + 269)
        } yield (l, CoapOptionOffset(2))
      case 15 => Left(InvalidOptionLength("15 is a reserved length value."))
      case other if 0 to 12 contains other => for {
          l <- CoapOptionLength(other)
       } yield (l, CoapOptionOffset(0))
      case e => Left(InvalidOptionLength(s"Illegal length value of ${e}. Initial value must be between 0 and 15"))
    }

  private def getValue(
    chunk: Chunk[Byte],
    length: CoapOptionLength,
    offset: CoapOptionOffset
  ): Either[InvalidCoapChunkSize, CoapOptionValue] = {
    val dropOptionHeader = chunk.drop(offset.value)
    if (dropOptionHeader.lengthCompare(length.value) >= 0) Right(CoapOptionValue(dropOptionHeader.take(length.value)))
    else Left(InvalidCoapChunkSize("Packet ended prematurely. Failed to extract an option value."))
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
  private def getMsgId(third: Byte, fourth: Byte): Int = ((third & 0xFF) << 8) | (fourth & 0xFF)
}