package domain.model.coap

import zio.Chunk

import scala.collection.immutable.HashMap


sealed trait CoapPayload

// TODO: REFACTOR THE APPLY METHODS TO RETURN AN EITHER?!
final case class TextCoapPayload private(value: String) extends CoapPayload

object TextCoapPayload { // Either[InvalidPayloadStructure,
  def apply(chunk: Chunk[Byte]): TextCoapPayload =
    new TextCoapPayload(chunk.map(_.toChar).mkString)
}
final case class UnknownPayload (value: Chunk[Byte]) extends CoapPayload

// TODO: Implement the other Media Types

sealed trait CoapPayloadMediaType

object CoapPayloadMediaType {
  def fromInt(ref: Int): CoapPayloadMediaType = references.getOrElse(ref, SniffingMediaType)

  private val references: Map[Int, CoapPayloadMediaType] = HashMap(
    0  -> TextMediaType,
    40 -> LinkMediaType,
    41 -> XMLMediaType,
    42 -> OctetStreamMediaType,
    47 -> EXIMediaType,
    50 -> JSONMediaType,
  )
}

case object TextMediaType        extends CoapPayloadMediaType
case object LinkMediaType        extends CoapPayloadMediaType
case object XMLMediaType         extends CoapPayloadMediaType
case object OctetStreamMediaType extends CoapPayloadMediaType
case object EXIMediaType         extends CoapPayloadMediaType
case object JSONMediaType        extends CoapPayloadMediaType

// DEFAULT FALLBACK
case object SniffingMediaType    extends CoapPayloadMediaType