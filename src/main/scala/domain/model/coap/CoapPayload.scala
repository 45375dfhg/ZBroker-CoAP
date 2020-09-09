package domain.model.coap

import zio.Chunk

final case class CoapPayload(coapPayloadContentFormat: CoapPayloadContentFormat, payload: Payload)

sealed trait Payload

final case class TextPayload private(value: String) extends Payload
object TextPayload {
  def apply(chunk: Chunk[Byte]): TextPayload = new TextPayload(chunk.map(_.toChar).mkString)
}