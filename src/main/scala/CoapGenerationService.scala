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

    def generateAllOptions(list: List[CoapOption]) = {

      def generateOneOption(option: CoapOption) = {

        def generateDelta(delta: CoapOptionDelta): Byte =
          (delta.value << 4).toByte

        def generateLength(length: CoapOptionLength): Byte =
          length.value.toByte

        def generateExtendedDelta(ext: Option[CoapExtendedDelta]): Chunk[Byte] =
          ext.fold(Chunk.empty)(a => if (a.value < 269) Chunk(a.value.toByte)
          else Chunk(((a.value >> 8) & 0xFF).toByte,(a.value & 0xFF).toByte))

        def generateExtendedLength(ext: Option[CoapExtendedLength]): Chunk[Byte] =
          ext.fold(Chunk.empty)(a => if (a.value < 269) Chunk(a.value.toByte)
          else Chunk(((a.value >> 8) & 0xFF).toByte,(a.value & 0xFF).toByte))

        // TODO: NOT FULLY IMPLEMENTED
        def generateValue(v: CoapOptionValue) =
          v.value

        val firstByte =
          (generateDelta(option.delta)
        + generateLength(option.length))

        val rest =
          generateExtendedDelta(option.exDelta) ++
          generateExtendedLength(option.exLength)


      }
    }

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
    def generateVersion(version: CoapVersion): Int =
      version.number << 6

    def generateType(msgType: CoapType): Int =
      msgType.number << 4

    def generateTokenLength(length: CoapTokenLength): Int =
      length.value

    def generateCodePrefix(prefix: CoapCodePrefix): Int =
      prefix.number << 5

    def generateCodeSuffix(suffix: CoapCodeSuffix): Int =
      suffix.number.toByte

    def generateMessageId(id: CoapId): Chunk[Int] =
      Chunk((id.value >> 8) & 0xFF, id.value & 0xFF)

    val firstByte: Int =
      generateVersion(head.version) + generateType(head.msgType) + generateTokenLength(head.tLength)

    val secondByte: Int = generateCodePrefix(head.cPrefix) + generateCodeSuffix(head.cSuffix)

    val thirdAndFourthByteAsChunk = generateMessageId(head.msgID)

    (firstByte +: (secondByte +: thirdAndFourthByteAsChunk)).map(_.toByte)
  }
}

