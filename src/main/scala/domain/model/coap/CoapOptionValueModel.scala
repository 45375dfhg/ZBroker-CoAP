package domain.model.coap

/*
 * The entry point is in {{{CoapMessage}}}
 */

import zio.Chunk

import scala.collection.immutable.HashMap

final case class CoapOptionValue(
  number: CoapOptionNumber,
  critical: Critical,
  unsafe: Unsafe,
  noCacheKey: NoCacheKey,
  repeatable: Repeatable,
  content: Content
)

final case class CoapOptionNumber private(value: Int) extends AnyVal { self =>
  import CoapOptionNumber._

  def getOptionFormat: CoapOptionFormat =
    map(self.value)._1

  def getOptionMaxLength: Range =
    map(self.value)._2
}

object CoapOptionNumber {

  // TODO: Add C-U-N-R parameters to the value
  val map: HashMap[Int, (CoapOptionFormat, Range)] = HashMap(
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

  val numbers = map.keySet

  def apply(value: Int): Either[CoapMessageException, CoapOptionNumber] =
    Either.cond(numbers contains value, new CoapOptionNumber(value), InvalidCoapOptionNumber)
}

sealed trait CoapOptionFormat
case object IntFormat    extends CoapOptionFormat
case object StringFormat extends CoapOptionFormat
case object OpaqueFormat extends CoapOptionFormat
case object EmptyFormat  extends CoapOptionFormat

sealed trait Content
final case class IntContent(value: Int)            extends Content
final case class StringContent(value: String)      extends Content
final case class OpaqueContent(value: Chunk[Byte]) extends Content
case object      EmptyContent                      extends Content

final case class Critical(value: Boolean)   extends AnyVal
final case class Unsafe(value: Boolean)     extends AnyVal
final case class NoCacheKey(value: Boolean) extends AnyVal
final case class Repeatable(value: Boolean) extends AnyVal