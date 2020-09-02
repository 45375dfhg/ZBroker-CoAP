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
    } yield CoapHeader(version, msgType, tLength, prefix, suffix, id) else Left(InvalidCoapChunkSize)

  private def bodyFromChunk(chunk: Chunk[Byte], header: CoapHeader): Either[CoapMessageException, CoapBody] = ???

  private def getVersion(b: Byte): Int = (b & 0xF0) >>> 6
  private def getMsgType(b: Byte): Int = (b & 0x30) >> 4
  private def getTLength(b: Byte): Int = b & 0x0F
  private def getCPrefix(b: Byte): Int = (b & 0xE0) >>> 5
  private def getCSuffix(b: Byte): Int = b & 0x1F
  private def getMsgId(third: Byte, fourth: Byte) = (third << 8) | (fourth & 0xff)
}