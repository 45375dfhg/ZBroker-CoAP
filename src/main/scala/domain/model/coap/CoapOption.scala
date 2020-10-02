package domain.model.coap

import option._

import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._

import zio._
import zio.Chunk

import utility.ChunkExtension._

import scala.collection.immutable.HashMap

import java.nio.ByteBuffer


final case class CoapOption(
  delta    : CoapOptionDelta,
  exDelta  : Option[CoapOptionExtendedDelta],
  length   : CoapOptionLength,
  exLength : Option[CoapOptionExtendedLength],
  number   : CoapOptionNumber,
  optValue : CoapOptionValue,
  offset   : CoapOptionOffset
)

package object option {

  @newtype class CoapOptionDelta private(val value: Int)

  object CoapOptionDelta {
    def apply(value: Int): IO[MessageFormatError, CoapOptionDelta] =
      // #rfc7252 accepts a 4-bit unsigned integer - 15 is reserved for the payload marker
      // ... while 13 and 14 lead to special constructs via ext8 and ext16
      IO.cond(0 to 15 contains value, value.coerce, InvalidOptionDelta(s"$value"))
  }

  @newtype class CoapOptionExtendedDelta private(val value: Int)

  object CoapOptionExtendedDelta {
    def apply(value: Int): IO[MessageFormatError, CoapOptionExtendedDelta] =
      // #rfc7252 accepts either 8 or 16 bytes as an extension to the small delta value.
      // The extension value must be greater than 12 which is a highest non special construct value.
      IO.cond(13 to 65804 contains value, value.coerce, InvalidOptionDelta(s"$value"))
  }

  @newtype class CoapOptionLength private(val value: Int)

  object CoapOptionLength {
    def apply(value: Int): IO[MessageFormatError, CoapOptionLength] = {
      // #rfc7252 accepts a 4-bit unsigned integer - 15 is reserved for the payload marker
      // ... while 13 and 14 lead to special constructs via ext8 and ext16
      // WARNING: This is an "open" bounded implementation! Errors must be caught on creation.
      IO.cond(0 to 65804 contains value, value.coerce, InvalidOptionLength(s"$value"))
    }
  }

  @newtype class CoapOptionExtendedLength private(val value: Int)

  object CoapOptionExtendedLength {
    def apply(value: Int): IO[MessageFormatError, CoapOptionExtendedLength] =
      // #rfc7252 accepts either 8 or 16 bytes as an extension to the small length value.
      // The extension value must be greater than 12 which is a highest non special construct value.
      IO.cond(13 to 65804 contains value, value.coerce, InvalidOptionLength(s"$value"))
  }

  @newtype case class CoapOptionOffset(value: Int)

  object CoapOptionOffset {
    implicit val numeric: Numeric[CoapOptionOffset] = deriving
  }

  @newtype class CoapOptionValue private(val content: CoapOptionValueContent)

  object CoapOptionValue {
    def apply(number: CoapOptionNumber, raw: Chunk[Byte]): CoapOptionValue =
      number.getOptionFormat.transform(raw, number.getOptionLengthRange).coerce
  }

  @newtype class CoapOptionNumber private(val value: Int) {

    def getOptionFormat: CoapOptionValueFormat =
      CoapOptionNumber.format(value)._1

    def getOptionLengthRange: Range =
      CoapOptionNumber.format(value)._2

    def getOptionProperties: (Boolean, Boolean, Boolean, Boolean) =
      CoapOptionNumber.properties(value)

    def isCritical: Boolean =
      CoapOptionNumber.properties(value)._1
  }

  object CoapOptionNumber {

    def getFormat(number: CoapOptionNumber): CoapOptionValueFormat =
      format(number.value)._1

    /**
     * Returns a quadruple of Booleans that represent the properties
     * Critical, Unsafe, NoCacheKey and Repeatable in the given order for the
     * given CoapOptionNumber
     */
    def getProperties(number: CoapOptionNumber): (Boolean, Boolean, Boolean, Boolean) =
      properties(number.value)

    // https://tools.ietf.org/html/rfc7959#section-2.1
    private val format: HashMap[Int, (CoapOptionValueFormat, Range)] = HashMap(
      1  -> (OpaqueOptionValueFormat, 0 to 8),
      3  -> (StringOptionValueFormat, 1 to 255),
      4  -> (OpaqueOptionValueFormat, 1 to 8),
      5  -> (EmptyOptionValueFormat,  0 to 0),
      7  -> (IntOptionValueFormat,    0 to 2),
      8  -> (StringOptionValueFormat, 0 to 255),
      11 -> (StringOptionValueFormat, 0 to 255),
      12 -> (IntOptionValueFormat,    0 to 2),
      14 -> (IntOptionValueFormat,    0 to 4),
      15 -> (StringOptionValueFormat, 0 to 255),
      17 -> (IntOptionValueFormat,    0 to 2),
      20 -> (StringOptionValueFormat, 0 to 255),
      23 -> (IntOptionValueFormat,    0 to 3),
      27 -> (IntOptionValueFormat,    0 to 3),
      35 -> (StringOptionValueFormat, 1 to 1034),
      39 -> (StringOptionValueFormat, 1 to 255),
      60 -> (IntOptionValueFormat,    0 to 4)
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
      23 -> (true,  true,  false, false),
      27 -> (true,  true,  false, false),
      35 -> (true,  true,  false, false),
      39 -> (true,  true,  false, false),
      60 -> (false, false, true,  true)
    )

    private val numbers = format.keySet

    def apply(value: Int): IO[MessageFormatError, CoapOptionNumber] =
      IO.cond(numbers contains value, value.coerce, InvalidCoapOptionNumber(s"$value"))
  }

  sealed trait CoapOptionValueFormat {
    def transform(raw: Chunk[Byte], range: Range): CoapOptionValueContent = this match {
      case IntOptionValueFormat    => IntCoapOptionValueContent(raw, range)
      case StringOptionValueFormat => StringCoapOptionValueContent(raw, range)
      case OpaqueOptionValueFormat => OpaqueCoapOptionValueContent(raw, range)
      case EmptyOptionValueFormat  => EmptyCoapOptionValueContent
    }
  }
  case object IntOptionValueFormat          extends CoapOptionValueFormat
  case object StringOptionValueFormat       extends CoapOptionValueFormat
  case object OpaqueOptionValueFormat       extends CoapOptionValueFormat
  case object EmptyOptionValueFormat        extends CoapOptionValueFormat


  sealed trait CoapOptionValueContent
  case object UnrecognizedValueContent          extends CoapOptionValueContent
  case object EmptyCoapOptionValueContent       extends CoapOptionValueContent

  final case class IntCoapOptionValueContent private(value: Int) extends CoapOptionValueContent

  object IntCoapOptionValueContent {
    def apply(raw: Chunk[Byte], range: Range): CoapOptionValueContent = {
      // TODO: Rework model as ZIO => Buffer.byte(raw.leftPadTo(4, 0.toByte)).map(_.getInt))
      if (range contains raw.size)
        new IntCoapOptionValueContent(ByteBuffer.wrap(raw.leftPadTo(4, 0.toByte).toArray).getInt)
      else UnrecognizedValueContent
    }
  }

  final case class StringCoapOptionValueContent private(value: String) extends CoapOptionValueContent

  object StringCoapOptionValueContent {
    def apply(raw: Chunk[Byte], range: Range): CoapOptionValueContent =
      if (range contains raw.size) new StringCoapOptionValueContent(new String(raw.toArray, "UTF-8"))
      else UnrecognizedValueContent
  }

  final case class OpaqueCoapOptionValueContent private(value: Chunk[Byte]) extends CoapOptionValueContent

  object OpaqueCoapOptionValueContent {
    def apply(raw: Chunk[Byte], range: Range): CoapOptionValueContent =
      if (range contains raw.size) new OpaqueCoapOptionValueContent(raw)
      else UnrecognizedValueContent
  }

  @newtype case class Critical(value: Boolean)
  @newtype case class Unsafe(value: Boolean)
  @newtype case class NoCacheKey(value: Boolean)
  @newtype case class Repeatable(value: Boolean)
}