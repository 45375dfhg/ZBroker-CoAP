package root

import domain.model.coap._
import zio.Chunk

/**
 * This service provides functionality to transform an internal representation of a CoAP message
 * into its respective low level form. The result type is of Chunk[Byte] and thus can be
 * transported to third parties via a ZIO-NIO channel
 */
object CoapGenerationService {
  def generateFromMessage(message: CoapMessage): Chunk[Byte] = {
    generateHeader(message.header) ++ generateBody(message.body)
  }

  // TODO: WARNING: TOKEN AND PAYLOAD MODEL NOT DONE AS OF NOW
  private def generateBody(body: CoapBody): Chunk[Byte] = {

    /**
     * Transforms all provides internal representations of CoapOptions
     * into
     */
    def generateAllOptions(list: List[CoapOption]): Chunk[Byte] = {

      def generateOneOption(option: CoapOption): Chunk[Byte] = {

        def generateDelta(delta: CoapOptionDelta): Int = delta.value << 4
        def generateLength(length: CoapOptionLength): Int = length.value

        def generateExtendedDelta(opt: Option[CoapExtendedDelta]): Chunk[Byte] =
          opt.fold(Chunk[Byte]()) { ext =>
            if (ext.value < 269) Chunk(ext.value.toByte)
            else Chunk(((ext.value >> 8) & 0xFF).toByte, (ext.value & 0xFF).toByte)
          }

        def generateExtendedLength(opt: Option[CoapExtendedLength]): Chunk[Byte] =
          opt.fold(Chunk[Byte]()){ ext =>
            if (ext.value < 269) Chunk(ext.value.toByte)
            else Chunk(((ext.value >> 8) & 0xFF).toByte,(ext.value & 0xFF).toByte)
          }

        // TODO: NOT FULLY IMPLEMENTED
        def generateValue(v: CoapOptionValue): Chunk[Byte] = v.value

        val firstByte: Byte = (generateDelta(option.delta) + generateLength(option.length)).toByte

        val rest: Chunk[Byte] =
          generateExtendedDelta(option.exDelta) ++
          generateExtendedLength(option.exLength) ++
            generateValue(option.value)

        firstByte +: rest
      }

      Chunk.fromArray(list.toArray).flatMap(generateOneOption)
    }

    val token =
      body.token match {
        case Some(t) => t.value
        case None => Chunk.empty
      }

    val options =
      body.options match {
        case Some(opts) => generateAllOptions(opts)
        case None => Chunk.empty
      }

    val payload =
      body.payload match {
        case Some(pay) => pay.value
        case None => Chunk.empty
      }

    token ++ options ++ payload
  }

  private def generateHeader(head: CoapHeader): Chunk[Byte] = {

    def generateVersion(version: CoapVersion): Int = version.number << 6

    def generateType(msgType: CoapType): Int = msgType.number << 4

    def generateTokenLength(length: CoapTokenLength): Int = length.value

    def generateCodePrefix(prefix: CoapCodePrefix): Int = prefix.number << 5

    def generateCodeSuffix(suffix: CoapCodeSuffix): Int = suffix.number.toByte

    def generateMessageId(id: CoapId): Chunk[Int] = Chunk((id.value >> 8) & 0xFF, id.value & 0xFF)

    val firstByte: Int =
      generateVersion(head.version) + generateType(head.msgType) + generateTokenLength(head.tLength)

    val secondByte: Int = generateCodePrefix(head.cPrefix) + generateCodeSuffix(head.cSuffix)

    val thirdAndFourthByteAsChunk = generateMessageId(head.msgID)

    // TODO: Check whether Chunk is closer to an Array or List performance-wise
    // most likely Array which means
    (firstByte +: (secondByte +: thirdAndFourthByteAsChunk)).map(_.toByte)
  }
}

