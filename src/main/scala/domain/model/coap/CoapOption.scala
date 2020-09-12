package domain.model.coap

import io.estatico.newtype.NewType
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._

import optionParameters._

final case class CoapOption(
  delta    : CoapOptionDelta,
  exDelta  : Option[CoapOptionExtendedDelta],
  length   : CoapOptionLength,
  exLength : Option[CoapOptionExtendedLength],
  value    : CoapOptionValue,
  offset   : CoapOptionOffset
)

package object optionParameters {

  @newtype class CoapOptionDelta private(val value: Int)

  object CoapOptionDelta {
    def apply(value: Int): Either[InvalidCoapMessage, CoapOptionDelta] =
      // #rfc7252 accepts a 4-bit unsigned integer - 15 is reserved for the payload marker
      // ... while 13 and 14 lead to special constructs via ext8 and ext16
      Either.cond(0 to 15 contains value, value.coerce, InvalidOptionDelta(s"$value"))
  }

  @newtype class CoapOptionExtendedDelta private(val value: Int)

  object CoapOptionExtendedDelta {
    def apply(value: Int): Either[InvalidCoapMessage, CoapOptionExtendedDelta] = {
      // #rfc7252 accepts either 8 or 16 bytes as an extension to the small delta value.
      // The extension value must be greater than 12 which is a highest non special construct value.
      Either.cond(13 to 65804 contains value, value.coerce, InvalidOptionDelta(s"$value"))
    }
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
}