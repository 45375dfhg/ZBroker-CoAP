package root

import domain.model.coap._
import zio.Chunk

/**
 * This service provides functionality to transform an internal representation of a CoAP message
 * into its respective low level form. The result type is of Chunk[Byte] and thus can be
 * transported to third parties via a ZIO-NIO channel
 */
object CoapGenerationService {
  def generateFromMessage(message: CoapMessage) = {

  }

  // TODO: WARNING TOKEN AND PAYLOAD MODEL NOT DONE AS OF NOW
  private def generateBody(body: CoapBody): Chunk[Byte] = {

    def generateAllOptions(list: List[CoapOption]) = ???

    def generateOneOption(option: CoapOption) = ???

    def generateDelta(delta: CoapOptionDelta): Byte = ???


    val token =
      body.token match {
        case Some(t) => t.value
        case None => Chunk.empty
      }

    val options =
      body.options match {
        case Some(opts) => opts
        case None => Chunk.empty
      }

    val payload =
      body.payload match {
        case Some(pay) => pay.value
        case None => Chunk.empty
      }

    ???
  }

  private def generateHeader(head: CoapHeader): Chunk[Byte] = {
    def generateVersion(version: CoapVersion): Byte =
      (version.number << 6).toByte

    def generateType(msgType: CoapType): Byte =
      (msgType.number << 4).toByte

    def generateTokenLength(length: CoapTokenLength): Byte =
      length.value.toByte

    def generateCodePrefix(prefix: CoapCodePrefix): Byte =
      (prefix.number << 5).toByte

    def generateCodeSuffix(suffix: CoapCodeSuffix): Byte =
      suffix.number.toByte

    def generateMessageId(id: CoapId): Chunk[Byte] =
      Chunk(((id.value >> 8) & 0xFF).toByte,(id.value & 0xFF).toByte)

    val firstByte =
      generateVersion(head.version)
    + generateType(head.msgType)
    + generateTokenLength(head.tLength)

    val secondByte: Byte =
      generateCodePrefix(head.cPrefix)
    + generateCodeSuffix(head.cSuffix)

    val thirdAndFourthChunk = generateMessageId(head.msgID)

    firstByte +: (secondByte +: thirdAndFourthChunk)
  }
}

