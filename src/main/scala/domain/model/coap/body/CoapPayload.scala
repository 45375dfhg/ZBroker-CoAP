package domain.model.coap.body

import java.nio.charset.StandardCharsets

import domain.model.coap.body.fields._
import domain.model.exception._
import utility.classExtension.ChunkExtension.ChunkExtension
import zio._

import scala.collection.immutable.HashMap

abstract class CoapPayload {
  def toByteChunk: Chunk[Byte] = this match {
    case p : TextCoapPayload => CoapPayload.marker +: Chunk.fromArray(p.value.getBytes(StandardCharsets.UTF_8))
    case _ => Chunk[Byte]()
  }
}

final case class TextCoapPayload private(value: String) extends CoapPayload

object TextCoapPayload {
  def apply(chunk: Chunk[Byte]): TextCoapPayload =
    new TextCoapPayload(new String(chunk.toArray, StandardCharsets.UTF_8))
}

final case class UnknownPayload (value: Chunk[Byte]) extends CoapPayload

object CoapPayload {

  /**
   * This function will drop an offset-number of elements from the body parameter. From the resulting Chunk is expected
   * that the now head element is a payload marker and the tail of the remainder is non-empty. According to specification
   * a payload marker without following payload is a MessageFormatError.
   * <p>
   * IMPORTANT: A wrong offset that leads to a non-payload-marker element in head position will fail this function -
   * a function without payload will only return a non-error when the offset leads to non-existing head element!
   * @param body The body of a CoapMessage or any Chunk[Byte] where the offset parameter allows access to a remainder
   *             that is headed by a marker byte.
   * @param format The CoapPayloadMediaType which is usually extracted from the CoapOptionList as an Option with #12.
   * @param offset The offset which is to be dropped off the body parameter to access the marker position.
   */
  def fromWithExcluding(body: Chunk[Byte], format: CoapPayloadMediaType, offset: Int = 0): IO[MessageFormatError, Option[CoapPayload]] =
    body.dropExactly(offset).flatMap(load => (load, load.headOption) match {
      case (load, Some(byte)) => if (byte == marker && load.tail.nonEmpty) IO.some(CoapPayload.fromWith(load.tail, format))
                                 else IO.fail(InvalidPayloadMarker)
      case (_, None)          => IO.none // technically an error?
    })

  private def fromWith(chunk: Chunk[Byte], format: CoapPayloadMediaType): CoapPayload =
    format match {
      case TextMediaType     => TextCoapPayload(chunk)
      case SniffingMediaType => TextCoapPayload(chunk) // placeholder
      case _                 => TextCoapPayload(chunk) // placeholder
    }

  // The payload marker as defined in the specification.
  private val marker = 0xFF.toByte
}

/**
 * This trait is the entry point for the CoapPayloadMediaType abstract data type.
 * The underlying MediaTypes define the Type of a CoapPayload. This is basically
 * an enumeration with some additional functionality.
 */
sealed trait CoapPayloadMediaType

object CoapPayloadMediaType {

  def fromOption(coapOptionList: Option[CoapOptionList]): CoapPayloadMediaType =
    coapOptionList match {
      case Some(value) => fromCoapOptionList(value)
      case None        => SniffingMediaType
    }

    def fromCoapOptionList(coapOptionList: CoapOptionList): CoapPayloadMediaType =
      coapOptionList.value.find(_.coapOptionNumber.value == 12) match {
        case Some(element) => element.coapOptionValue.content match {
          case c : IntCoapOptionValueContent => CoapPayloadMediaType.fromInt(c.value)
          case _                             => SniffingMediaType // technically this is unexpected parsing error!
        }
      case None => SniffingMediaType
    }

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