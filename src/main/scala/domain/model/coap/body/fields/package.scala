package domain.model.coap.body

import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

import domain.model.exception._
import io.estatico.newtype.macros._
import io.estatico.newtype.ops._
import utility.ChunkExtension._
import utility.Extractor
import utility.Extractor._
import zio._

import scala.collection.immutable.HashMap

package object fields {

  @newtype class CoapOptionDelta private(val value: Int) {
    def offset = value match {
      case zero if 0   to 12    contains zero => 0
      case one  if 13  to 268   contains one  => 1
      case two  if 269 to 65804 contains two  => 2
    }

    def toOptionHeader: Int = getHeaderByte(this.value)

    def toOptionBodyExt: Chunk[Byte] = getExtensionFrom(this.value)
  }

  object CoapOptionDelta {
    def apply(value: Int): IO[MessageFormatError, CoapOptionDelta] =
      IO.cond(0 to 65804 contains value, value.coerce, InvalidOptionDelta(s"$value"))

    def fromWith(header: Byte, body: Chunk[Byte]): IO[GatewayError, CoapOptionDelta] =
      CoapOptionDelta.fromOptionHeader(header).flatMap(CoapOptionDelta.extend(_, body))

    // #rfc7252 accepts a 4-bit unsigned integer - 15 is reserved for the payload marker
    def fromOptionHeader(header: Byte): IO[InvalidOptionDelta, CoapOptionDelta] =
      (header & 0xF0) >>> 4 match {
        case basic if 0 to 14 contains basic => IO.succeed(basic.coerce)
        case err                             => IO.fail(InvalidOptionDelta(s"$err"))
      }

    // ... while 13 and 14 lead to special constructs via ext8 and ext16
    private def extend(coapOptionDelta: CoapOptionDelta, body: Chunk[Byte]): ZIO[Any, GatewayError, CoapOptionDelta] =
      coapOptionDelta.value match {
        case basic if 0 to 12 contains basic => IO.succeed(coapOptionDelta)
        case 13 => body.takeExactly(1).map(_.head) >>= (n => CoapOptionDelta(n + 13)) // TODO: head.toInt?
        case 14 => body.takeExactly(2).map(merge)  >>= (n => CoapOptionDelta(n + 269))
        case _  => IO.fail(UnreachableCodeError) // TODO: Rethink this error!
      }
  }

  @newtype class CoapOptionLength private(val value: Int) {
    def offset = value match {
      case zero if 0   to 12    contains zero => 0
      case one  if 13  to 268   contains one  => 1
      case two  if 269 to 65804 contains two  => 2
    }

    def toOptionHeader: Int = getHeaderByte(this.value)

    def toOptionBodyExt: Chunk[Byte] = getExtensionFrom(this.value)
  }

  object CoapOptionLength {
    def apply(value: Int): IO[MessageFormatError, CoapOptionLength] =
      IO.cond(0 to 65804 contains value, value.coerce, InvalidOptionLength(s"$value"))

    def fromWithExcluding(header: Byte, body: Chunk[Byte], offset: Int): IO[GatewayError, CoapOptionLength] =
      fromOptionHeader(header).flatMap(extend(_, body, offset))

    // #rfc7252 accepts a 4-bit unsigned integer - 15 is reserved for the payload marker
    def fromOptionHeader(header: Byte): IO[InvalidOptionDelta, CoapOptionLength] =
      header & 0x0F match {
        case basic if 0 to 14 contains basic => IO.succeed(basic.coerce)
        case err                             => IO.fail(InvalidOptionDelta(s"$err"))
      }

    // ... while 13 and 14 lead to special constructs via ext8 and ext16
    private def extend(coapOptionLength: CoapOptionLength, body: Chunk[Byte], offset: Int): IO[GatewayError, CoapOptionLength] =
      coapOptionLength.value match {
        case basic if 0 to 12 contains basic => IO.succeed(coapOptionLength)
        case 13 => body.dropExactly(offset).flatMap(_.takeExactly(1)).map(_.head) >>= (n => CoapOptionLength(n + 13)) // TODO: head.toInt?
        case 14 => body.dropExactly(offset).flatMap(_.takeExactly(2)).map(merge)  >>= (n => CoapOptionLength(n + 269))
        case _  => IO.fail(UnreachableCodeError) // TODO: Rethink this error!
      }
  }

  def getHeaderByte(optionHeaderParam: Int): Int =
    optionHeaderParam match {
      case v if 0   to 12    contains v => v
      case v if 13  to 268   contains v => 13
      case v if 269 to 65804 contains v => 14
      case _                            => 15
    }

  def getExtensionFrom(optionHeaderParam: Int): Chunk[Byte] =
    optionHeaderParam match {
      case v if 0   to 12    contains v => Chunk.empty
      case v if 13  to 268   contains v => Chunk((v - 13).toByte)
      case v if 269 to 65804 contains v => Chunk((((v - 269) >> 8) & 0xFF).toByte, ((v - 269) & 0xFF).toByte)
      case _                            => Chunk.empty // TODO: value can't be higher than 65804 or lower than 0!
    }

  @newtype class CoapOptionNumber private(val value: Int) {

    def getOptionFormat: CoapOptionValueFormat =
      CoapOptionNumber.getFormat(value)._1

    def getOptionLengthRange: Range =
      CoapOptionNumber.getFormat(value)._2

    def getOptionProperties: (Boolean, Boolean, Boolean, Boolean) =
      CoapOptionNumber.properties(value)

    def isCritical: Boolean =
      CoapOptionNumber.properties(value)._1
  }

  object CoapOptionNumber {

    def getFormat(number: CoapOptionNumber): CoapOptionValueFormat =
      getFormat(number.value)._1

    /**
     * Returns a quadruple of Booleans that represent the properties
     * Critical, Unsafe, NoCacheKey and Repeatable in the given order for the
     * given CoapOptionNumber
     */
    def getProperties(number: CoapOptionNumber): (Boolean, Boolean, Boolean, Boolean) =
      properties(number.value)

    /**
     * Each CoapOptionNumber is mapped to a format. The CoapOptionNumber is represented as an Int.
     * Each mapping also includes a value range which further specifies the CoapOption's size range.
     * <p>
     * Values are based on: https://tools.ietf.org/html/rfc7959#section-2.1
     */
    private val getFormat: HashMap[Int, (CoapOptionValueFormat, Range)] = HashMap(
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

    private val numbers = getFormat.keySet

    private def apply(value: Int): IO[MessageFormatError, CoapOptionNumber] =
      IO.cond(numbers contains value, value.coerce, InvalidCoapOptionNumber(s"$value"))

    def from(value: Int): IO[MessageFormatError, CoapOptionNumber] =
      apply(value)
  }

  /**
   * A CoapOptionValue's format. The format can either be of type Int, String, Opaque or plain empty.
   */
  sealed trait CoapOptionValueFormat {
    def applyOnWith(raw: Chunk[Byte], range: Range): CoapOptionValueContent = this match {
      case IntOptionValueFormat    => IntCoapOptionValueContent(raw, range)
      case StringOptionValueFormat => StringCoapOptionValueContent(raw, range)
      case OpaqueOptionValueFormat => OpaqueCoapOptionValueContent(raw, range)
      case EmptyOptionValueFormat  => EmptyCoapOptionValueContent
    }
  }

  case object IntOptionValueFormat    extends CoapOptionValueFormat
  case object StringOptionValueFormat extends CoapOptionValueFormat
  case object OpaqueOptionValueFormat extends CoapOptionValueFormat
  case object EmptyOptionValueFormat  extends CoapOptionValueFormat

  @newtype class CoapOptionValue private(val content: CoapOptionValueContent) {
    def toByteChunk: Chunk[Byte] = content match {
      case c : IntCoapOptionValueContent     => Chunk.fromByteBuffer(ByteBuffer.allocate(4).putInt(c.value).compact)
      case c : StringCoapOptionValueContent  => Chunk.fromArray(c.value.getBytes(StandardCharsets.UTF_8))
      case c : OpaqueCoapOptionValueContent  => c.value
      case EmptyCoapOptionValueContent       => Chunk.empty
      case UnrecognizedValueContent          => Chunk.empty
    }
  }

  object CoapOptionValue {
    def fromWith(raw: Chunk[Byte], number: CoapOptionNumber): CoapOptionValue =
      number.getOptionFormat.applyOnWith(raw, number.getOptionLengthRange).coerce

    def fromWithExcluding(
      body   : Chunk[Byte],
      length : CoapOptionLength,
      number : CoapOptionNumber,
      offset : Int,
    ): IO[MessageFormatError, CoapOptionValue] =
      body.dropExactly(offset).flatMap(_.takeExactly(length.value)).map(CoapOptionValue.fromWith(_, number))
  }

  sealed abstract class CoapOptionValueContent {
    def fromWith(chunk: Chunk[Byte], range: Range, coapOptionValueFormat: CoapOptionValueFormat) =
      coapOptionValueFormat match {
        case IntOptionValueFormat    => IntCoapOptionValueContent(chunk, range)
        case StringOptionValueFormat => StringCoapOptionValueContent(chunk, range)
        case OpaqueOptionValueFormat => OpaqueCoapOptionValueContent(chunk, range)
        case EmptyOptionValueFormat  => EmptyCoapOptionValueContent
      }
  }

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

  case object UnrecognizedValueContent    extends CoapOptionValueContent
  case object EmptyCoapOptionValueContent extends CoapOptionValueContent

  // TODO: Not in-use yet
  @newtype case class Critical(value: Boolean)
  @newtype case class Unsafe(value: Boolean)
  @newtype case class NoCacheKey(value: Boolean)
  @newtype case class Repeatable(value: Boolean)

  /**
   * A small helper functions that takes a Chunk[Byte] and merges the first two Bytes into an Int
   * There is no previous check on the index access validity so an unhandled error is possible.
   */
  private val merge = (c: Chunk[Byte]) => ((c(0) << 8) & 0xFF) | (c(1) & 0xFF)

  // REMOVE BELOW
  // REMOVE BELOW
  // REMOVE BELOW
  // REMOVE BELOW

  @newtype class CoapOptionExtendedDelta private(val value: Int)

  object CoapOptionExtendedDelta {
    def apply(value: Int): IO[MessageFormatError, CoapOptionExtendedDelta] =
    // #rfc7252 accepts either 8 or 16 bytes as an extension to the small delta value.
    // The extension value must be greater than 12 which is a highest non special construct value.
      IO.cond(13 to 65804 contains value, value.coerce, InvalidOptionDelta(s"$value"))
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
}
