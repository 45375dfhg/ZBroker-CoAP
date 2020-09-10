package domain.model.coap

import zio.Chunk

final case class CoapPayload(coapPayloadContentFormat: CoapPayloadMediaTypes, payloadContent: CoapPayloadContent)

sealed trait CoapPayloadContent

final case class TextCoapPayloadContent private(value: String) extends CoapPayloadContent
object TextCoapPayloadContent {
  def apply(chunk: Chunk[Byte]): TextCoapPayloadContent = new TextCoapPayloadContent(chunk.map(_.toChar).mkString)
}

/**
 * The Media Format of an
 */
sealed trait CoapPayloadMediaTypes
case object TextMediaType        extends CoapPayloadMediaTypes
case object LinkMediaType        extends CoapPayloadMediaTypes
case object XMLMediaType         extends CoapPayloadMediaTypes
case object OctetStreamMediaType extends CoapPayloadMediaTypes
case object EXIMediaType         extends CoapPayloadMediaTypes
case object JSONMediaType        extends CoapPayloadMediaTypes

// DEFAULT FALLBACK
case object SniffingMediaType    extends CoapPayloadMediaTypes

// TODO: Implement the other Media Types