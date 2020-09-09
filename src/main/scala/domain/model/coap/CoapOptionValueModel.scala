package domain.model.coap

/*
 * The entry point is in {{{CoapMessage}}}
 */

import java.nio.ByteBuffer

import zio.Chunk

import scala.collection.immutable.HashMap

final case class CoapOptionValue private(
  number: CoapOptionNumber,
  content: Content
)

object CoapOptionValue {
  def apply(
    number: CoapOptionNumber,
    raw: Chunk[Byte]
  ): Either[CoapMessageException, CoapOptionValue] = {
    val range = number.getOptionLengthRange
    (number.getOptionFormat match {
      case IntFormat    => IntContent(raw, range)
      case StringFormat => StringContent(raw, range)
      case OpaqueFormat => OpaqueContent(raw, range)
      case EmptyFormat  => EmptyContent(raw)
    }).map(content => CoapOptionValue(number, content))
  }
}

final case class CoapOptionNumber private(value: Int) extends AnyVal { self =>
  import CoapOptionNumber._

  def getOptionFormat: CoapOptionFormat =
    format(self.value)._1

  def getOptionLengthRange: Range =
    format(self.value)._2

  def getOptionProperties: (Boolean, Boolean, Boolean, Boolean) =
    properties(self.value)
}

object CoapOptionNumber {

  private val format: HashMap[Int, (CoapOptionFormat, Range)] = HashMap(
    1  -> (OpaqueFormat, 0 to 8),
    3  -> (StringFormat, 1 to 255),
    4  -> (OpaqueFormat, 1 to 8),
    5  -> (EmptyFormat,  0 to 0),
    7  -> (IntFormat,    0 to 2),
    8  -> (StringFormat, 0 to 255),
    11 -> (StringFormat, 0 to 255),
    12 -> (IntFormat,    0 to 2),
    14 -> (IntFormat,    0 to 4),
    15 -> (StringFormat, 0 to 255),
    17 -> (IntFormat,    0 to 2),
    20 -> (StringFormat, 0 to 255),
    35 -> (StringFormat, 1 to 1034),
    39 -> (StringFormat, 1 to 255),
    60 -> (IntFormat,    0 to 4)
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

  def apply(value: Int): Either[CoapMessageException, CoapOptionNumber] =
    Either.cond(numbers contains value, new CoapOptionNumber(value), InvalidCoapOptionNumber)
}

sealed trait CoapOptionFormat
case object IntFormat    extends CoapOptionFormat
case object StringFormat extends CoapOptionFormat
case object OpaqueFormat extends CoapOptionFormat
case object EmptyFormat  extends CoapOptionFormat

sealed trait Content

final case class IntContent private(value: Int) extends Content
object IntContent {
  def apply(raw: Chunk[Byte], range: Range): Either[CoapMessageException, Content] = {
    Either.cond(range contains raw.size, new IntContent(ByteBuffer.wrap(raw.toArray).getInt), InvalidCoapOptionLength)
  }
}

final case class StringContent private(value: String) extends Content
object StringContent {
  def apply(raw: Chunk[Byte], range: Range): Either[CoapMessageException, Content] = {
    Either.cond(range contains raw.size, new StringContent(raw.map(_.toChar).mkString), InvalidCoapOptionLength)
  }
}

final case class OpaqueContent private(value: Chunk[Byte]) extends Content
object OpaqueContent {
  def apply(raw: Chunk[Byte], range: Range): Either[CoapMessageException, Content] = {
    Either.cond(range contains raw.size, new OpaqueContent(raw), InvalidCoapOptionLength)
  }
}

// TODO: Refactor - this should be a case object instead.
final case class EmptyContent private(value: Chunk[Byte]) extends Content
object EmptyContent {
  def apply(raw: Chunk[Byte]): Either[CoapMessageException, Content] = {
    Either.cond(raw.isEmpty, new EmptyContent(Chunk[Byte]()), InvalidCoapOptionLength)
  }
}

final case class Critical(value: Boolean)   extends AnyVal
final case class Unsafe(value: Boolean)     extends AnyVal
final case class NoCacheKey(value: Boolean) extends AnyVal
final case class Repeatable(value: Boolean) extends AnyVal