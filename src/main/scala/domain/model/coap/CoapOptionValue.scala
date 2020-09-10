package domain.model.coap

/*
 * The entry point is in {{{CoapMessage}}}
 */

import java.nio.ByteBuffer

import zio.Chunk

import scala.collection.immutable.HashMap

final case class CoapOptionValue private(number: CoapOptionNumber, content: CoapOptionContent)

object CoapOptionValue {
  def apply(number: CoapOptionNumber, raw: Chunk[Byte]): CoapOptionValue = {
    val range = number.getOptionLengthRange

    CoapOptionValue(number, number.getOptionFormat match {
      case IntOptionFormat    => IntCoapOptionContent(raw, range)
      case StringOptionFormat => StringCoapOptionContent(raw, range)
      case OpaqueOptionFormat => OpaqueCoapOptionContent(raw, range)
      case EmptyOptionFormat  => EmptyCoapOptionContent
    })
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

  def apply(value: Int): Either[CoapMessageException, CoapOptionNumber] =
    Either.cond(numbers contains value, new CoapOptionNumber(value), InvalidCoapOptionNumber)
}

/**
 * The value of an Option can be written in one of four formats which are portrayed via this ADT
 */
sealed trait CoapOptionFormat

case object IntOptionFormat          extends CoapOptionFormat
case object StringOptionFormat       extends CoapOptionFormat
case object OpaqueOptionFormat       extends CoapOptionFormat
case object EmptyOptionFormat        extends CoapOptionFormat


/**
 * The content of an Option can be one of four different types.
 */
sealed trait CoapOptionContent
/**
 * If the length of an option value in a request is
 * outside the defined range, that option MUST be
 * treated like an unrecognized option - this is not an error or exception!
 */
case object UnrecognizedCoapOptionFormat extends CoapOptionContent
case object EmptyCoapOptionContent       extends CoapOptionContent

final case class IntCoapOptionContent private(value: Int) extends CoapOptionContent
object IntCoapOptionContent {
  def apply(raw: Chunk[Byte], range: Range): CoapOptionContent =
    if (range contains raw.size) new IntCoapOptionContent(ByteBuffer.wrap(raw.toArray).getInt)
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

final case class Critical(value: Boolean)   extends AnyVal
final case class Unsafe(value: Boolean)     extends AnyVal
final case class NoCacheKey(value: Boolean) extends AnyVal
final case class Repeatable(value: Boolean) extends AnyVal