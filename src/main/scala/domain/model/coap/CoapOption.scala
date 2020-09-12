package domain.model.coap

import java.nio.ByteBuffer

import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._
import zio.Chunk
import option._
import utility.ChunkExtension.ChunkExtension

import scala.collection.immutable.HashMap


final case class CoapOption(
  delta    : CoapOptionDelta,
  exDelta  : Option[CoapOptionExtendedDelta],
  length   : CoapOptionLength,
  exLength : Option[CoapOptionExtendedLength],
  value    : CoapOptionValue,
  offset   : CoapOptionOffset
)

package object option {

  @newtype class CoapOptionDelta private(val value: Int)

  object CoapOptionDelta {
    def apply(value: Int): Either[InvalidCoapMessage, CoapOptionDelta] =
      // #rfc7252 accepts a 4-bit unsigned integer - 15 is reserved for the payload marker
      // ... while 13 and 14 lead to special constructs via ext8 and ext16
      Either.cond(0 to 15 contains value, value.coerce, InvalidOptionDelta(s"$value"))
  }

  @newtype class CoapOptionExtendedDelta private(val value: Int)

  object CoapOptionExtendedDelta {
    def apply(value: Int): Either[InvalidCoapMessage, CoapOptionExtendedDelta] =
      // #rfc7252 accepts either 8 or 16 bytes as an extension to the small delta value.
      // The extension value must be greater than 12 which is a highest non special construct value.
      Either.cond(13 to 65804 contains value, value.coerce, InvalidOptionDelta(s"$value"))
  }

  @newtype class CoapOptionLength private(val value: Int)

  object CoapOptionLength {
    def apply(value: Int): Either[InvalidCoapMessage, CoapOptionLength] =
      // #rfc7252 accepts a 4-bit unsigned integer - 15 is reserved for the payload marker
      // ... while 13 and 14 lead to special constructs via ext8 and ext16
      Either.cond(0 to 15 contains value, value.coerce, InvalidOptionLength(s"$value"))
  }

  @newtype class CoapOptionExtendedLength private(val value: Int)

  object CoapOptionExtendedLength {
    def apply(value: Int): Either[InvalidCoapMessage, CoapOptionExtendedLength] =
      // #rfc7252 accepts either 8 or 16 bytes as an extension to the small length value.
      // The extension value must be greater than 12 which is a highest non special construct value.
      Either.cond(13 to 65804 contains value, value.coerce, InvalidOptionLength(s"$value"))
  }

  @newtype case class CoapOptionOffset(value: Int)

  object CoapOptionOffset {
    implicit val numeric: Numeric[CoapOptionOffset] = deriving
  }

  final case class CoapOptionValue private(number: CoapOptionNumber, content: CoapOptionContent)

  object CoapOptionValue {
    def apply(number: CoapOptionNumber, raw: Chunk[Byte]): CoapOptionValue =
      new CoapOptionValue(number, number.getOptionFormat.transform(raw, number.getOptionLengthRange))
  }

  @newtype class CoapOptionNumber private(val value: Int) {

    def getOptionFormat: CoapOptionFormat =
      CoapOptionNumber.format(value)._1

    def getOptionLengthRange: Range =
      CoapOptionNumber.format(value)._2

    def getOptionProperties: (Boolean, Boolean, Boolean, Boolean) =
      CoapOptionNumber.properties(value)

    def isCritical: Boolean =
      CoapOptionNumber.properties(value)._1
  }

  object CoapOptionNumber {

    def getFormat(number: CoapOptionNumber): CoapOptionFormat =
      format(number.value)._1

    /**
     * Returns a quadruple of Booleans that represent the properties
     * Critical, Unsafe, NoCacheKey and Repeatable in the given order for the
     * given CoapOptionNumber
     */
    def getProperties(number: CoapOptionNumber): (Boolean, Boolean, Boolean, Boolean) =
      properties(number.value)

    private val format: HashMap[Int, (CoapOptionFormat, Range)] = HashMap(
      1  -> (OpaqueOptionFormat, 0 to 8),
      3  -> (StringOptionFormat, 1 to 255),
      4  -> (OpaqueOptionFormat, 1 to 8),
      5  -> (EmptyOptionFormat,  0 to 0),
      7  -> (IntOptionFormat,    0 to 2),
      8  -> (StringOptionFormat, 0 to 255),
      11 -> (StringOptionFormat, 0 to 255),
      12 -> (IntOptionFormat,    0 to 2),
      14 -> (IntOptionFormat,    0 to 4),
      15 -> (StringOptionFormat, 0 to 255),
      17 -> (IntOptionFormat,    0 to 2),
      20 -> (StringOptionFormat, 0 to 255),
      35 -> (StringOptionFormat, 1 to 1034),
      39 -> (StringOptionFormat, 1 to 255),
      60 -> (IntOptionFormat,    0 to 4)
    )

    private val properties = HashMap(
      1  -> (true,  false, false, true),
      3  -> (true,  true,  false, false),
      4  -> (false, false, false, true),
      5  -> (true,  false, false, true),
      7  -> (true,  true,  false, false),
      8  -> (false, false, false, true),
      11 -> (true,  true,  false, true),
      12 -> (false, false, false, false),
      14 -> (false, true,  false, false),
      15 -> (true,  true,  false, true),
      17 -> (true,  false, false, false),
      20 -> (false, false, false, true),
      35 -> (true,  true,  false, false),
      39 -> (true,  true,  false, false),
      60 -> (false, false, true,  true)
    )

    val numbers = format.keySet

    def apply(value: Int): Either[InvalidCoapMessage, CoapOptionNumber] =
      Either.cond(numbers contains value, value.coerce, InvalidCoapOptionNumber(s"$value"))
  }

  sealed trait CoapOptionFormat {
    def transform(raw: Chunk[Byte], range: Range): CoapOptionContent = this match {
      case IntOptionFormat    => IntCoapOptionContent(raw, range)
      case StringOptionFormat => StringCoapOptionContent(raw, range)
      case OpaqueOptionFormat => OpaqueCoapOptionContent(raw, range)
      case EmptyOptionFormat  => EmptyCoapOptionContent
    }
  }
  case object IntOptionFormat          extends CoapOptionFormat
  case object StringOptionFormat       extends CoapOptionFormat
  case object OpaqueOptionFormat       extends CoapOptionFormat
  case object EmptyOptionFormat        extends CoapOptionFormat


  sealed trait CoapOptionContent
  case object UnrecognizedCoapOptionFormat extends CoapOptionContent
  case object EmptyCoapOptionContent       extends CoapOptionContent

  final case class IntCoapOptionContent private(value: Int) extends CoapOptionContent

  object IntCoapOptionContent {
    def apply(raw: Chunk[Byte], range: Range): CoapOptionContent =
      if (range contains raw.size) new IntCoapOptionContent(ByteBuffer.wrap(raw.leftPadTo(4, 0.toByte).toArray).getInt)
      else UnrecognizedCoapOptionFormat
  }

  final case class StringCoapOptionContent private(value: String) extends CoapOptionContent

  object StringCoapOptionContent {
    def apply(raw: Chunk[Byte], range: Range): CoapOptionContent =
      if (range contains raw.size) new StringCoapOptionContent(raw.map(_.toChar).mkString)
      else UnrecognizedCoapOptionFormat
  }

  final case class OpaqueCoapOptionContent private(value: Chunk[Byte]) extends CoapOptionContent

  object OpaqueCoapOptionContent {
    def apply(raw: Chunk[Byte], range: Range): CoapOptionContent =
      if (range contains raw.size) new OpaqueCoapOptionContent(raw)
      else UnrecognizedCoapOptionFormat
  }

  @newtype case class Critical(value: Boolean)
  @newtype case class Unsafe(value: Boolean)
  @newtype case class NoCacheKey(value: Boolean)
  @newtype case class Repeatable(value: Boolean)
}