package root

import domain.model.coap._

import utility.Extractor
import utility.Extractor._

import zio.Chunk

/**
 * This service provides functionality to transform an internal representation of a CoAP message
 * into its respective low level form. The result type is of Chunk[Byte] and thus can be
 * transported to third parties via a ZIO-NIO channel
 */
object CoapGenerationService {

  def generateFromMessage(message: CoapMessage): Chunk[Byte] =
    generateHeader(message.header) ++ generateBody(message.body)

  // TODO: WARNING: TOKEN AND PAYLOAD MODEL NOT DONE AS OF NOW
  private def generateBody(body: CoapBody): Chunk[Byte] = {

    /**
     * Transforms all provides internal representations of CoapOptions
     * into a Chunk of Byte. Since the internal rep. is checked on creation
     * for correction, this transformation assumes correct values.
     */
    def generateAllOptions(list: List[CoapOption]): Chunk[Byte] = {

      def generateOneOption(option: CoapOption): Chunk[Byte] =
        (getDeltaFrom(option.delta) + getLengthFrom(option.length)).toByte +:
          (getExtensionFrom(option.exDelta) ++ getExtensionFrom(option.exLength) ++ getValueFrom(option.value))

      Chunk.fromArray(list.toArray).flatMap(generateOneOption)
    }

    (body.token match {
      case Some(t) => t.value
      case None => Chunk.empty
    }) ++ (body.options match {
        case Some(opts) => generateAllOptions(opts)
        case None => Chunk.empty
      }) ++ (body.payload match {
          case Some(pay) => pay.value
          case None => Chunk.empty
        })
  }

  private def generateHeader(head: CoapHeader): Chunk[Byte] = {

    val firstByte: Int =
      generateVersion(head.version) + generateType(head.msgType) + generateTokenLength(head.tLength)

    val secondByte: Int = generateCodePrefix(head.cPrefix) + generateCodeSuffix(head.cSuffix)

    val thirdAndFourthByteAsChunk = generateMessageId(head.msgID)

    // TODO: Check whether Chunk is closer to an Array or List performance-wise
    // most likely Array which means
    (firstByte +: (secondByte +: thirdAndFourthByteAsChunk)).map(_.toByte)
  }

  private def getDeltaFrom(delta: CoapOptionDelta): Int =
    delta.value << 4

  private def getLengthFrom(length: CoapOptionLength): Int =
    length.value

  private def getExtensionFrom[A : Extractor](opt: Option[A]): Chunk[Byte] =
    opt.fold(Chunk[Byte]()) { e =>
      if (e.extract < 269) Chunk(e.extract.toByte)
      else Chunk(((e.extract >> 8) & 0xFF).toByte, (e.extract & 0xFF).toByte)
    }

  // TODO: NOT FULLY IMPLEMENTED
  private def getValueFrom(v: CoapOptionValue): Chunk[Byte] = v.value

  private def generateVersion(version: CoapVersion): Int = version.number << 6

  private def generateType(msgType: CoapType): Int = msgType.number << 4

  private def generateTokenLength(length: CoapTokenLength): Int = length.value

  private def generateCodePrefix(prefix: CoapCodePrefix): Int = prefix.number << 5

  private def generateCodeSuffix(suffix: CoapCodeSuffix): Int = suffix.number.toByte

  private def generateMessageId(id: CoapId): Chunk[Int] = Chunk((id.value >> 8) & 0xFF, id.value & 0xFF)
}

