package domain.model.coap.body


import fields._
import zio.Chunk

final case class CoapOption(
  coapOptionDelta  : CoapOptionDelta,
  coapOptionLength : CoapOptionLength,
  coapOptionNumber : CoapOptionNumber,
  coapOptionValue  : CoapOptionValue,
) {
  def toByteChunk: Chunk[Byte] =
    (coapOptionDelta.toOptionHeader + coapOptionLength.toOptionHeader).toByte +:
      (coapOptionDelta.toOptionBodyExt ++ coapOptionLength.toOptionBodyExt ++ coapOptionValue.toByteChunk)
}

