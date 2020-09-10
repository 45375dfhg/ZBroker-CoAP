package domain.model.coap

import zio.Chunk

final case class CoapPayload(coapPayloadContentFormat: CoapPayloadMediaTypes, payloadContent: CoapPayloadContent)

sealed trait CoapPayloadContent

final case class TextCoapPayloadContent private(value: String) extends CoapPayloadContent
object TextCoapPayloadContent {
  def apply(chunk: Chunk[Byte]): TextCoapPayloadContent = new TextCoapPayloadContent(chunk.map(_.toChar).mkString)
}

// TODO: Implement the other Media Types