package domain.model.coap

import zio.Chunk

final case class CoapPayload(coapPayloadContentFormat: CoapPayloadMediaType, payloadContent: CoapPayloadContent)

sealed trait CoapPayloadContent

final case class TextCoapPayloadContent private(value: String) extends CoapPayloadContent
object TextCoapPayloadContent {
  def apply(chunk: Chunk[Byte]): TextCoapPayloadContent = new TextCoapPayloadContent(chunk.map(_.toChar).mkString)
}

/**
 * The Media Format of an
 */
sealed trait CoapPayloadMediaType
case object TextMediaType        extends CoapPayloadMediaType
case object LinkMediaType        extends CoapPayloadMediaType
case object XMLMediaType         extends CoapPayloadMediaType
case object OctetStreamMediaType extends CoapPayloadMediaType
case object EXIMediaType         extends CoapPayloadMediaType
case object JSONMediaType        extends CoapPayloadMediaType

// DEFAULT FALLBACK
case object SniffingMediaType    extends CoapPayloadMediaType

// TODO: Implement the other Media Types