package root

import domain.model.coap._
import zio.Chunk

/**
 * This service transforms instances of internal CoAP representations to valid CoAP messages
 * in the Chunk[Byte] format to be sent to third parties.
 */
object CoapGenerationService {
  def generateFromMessage(message: CoapMessage) = {

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



//trait Extract[A <: AnyVal] {
//  def extract(param: A): Int
//}
//
//object Extract {
//
//  def extract[A: Extract](param: A)(implicit ex: Extract[A]) =
//    ex.extract(param)
//
//  implicit class ExtractOps[A: Extract](param: A) {
//    def extract: Int = Extract[A].extract(param)
//  }
//
//  def apply[A](implicit ex: Extract[A]): Extract[A] = ex
//
//  implicit val extractVersion: Extract[CoapVersion] =
//    (param: CoapVersion) => param.number
//
//  implicit val extractType: Extract[CoapType] =
//    (param: CoapType) => param.number
//
//  implicit val extractTokenLength: Extract[CoapTokenLength] =
//    (param: CoapTokenLength) => param.value
//
//  implicit val extractCodePrefix: Extract[CoapCodePrefix] =
//    (param: CoapCodePrefix) => param.number
//
//  implicit val extractCodeSuffix: Extract[CoapCodeSuffix] =
//    (param: CoapCodeSuffix) => param.number
//
//  implicit val extractCodeId: Extract[CoapId] =
//    (param: CoapId) => param.value
//}

