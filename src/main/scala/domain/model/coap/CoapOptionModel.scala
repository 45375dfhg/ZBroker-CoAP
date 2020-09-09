package domain.model.coap

/*
 * The entry point is in {{{CoapMessage}}}
 */

final case class CoapOption(
  delta    : CoapOptionDelta,  // internal?
  exDelta  : Option[CoapOptionExtendedDelta],
  length   : CoapOptionLength, // internal!
  exLength : Option[CoapOptionExtendedLength],
  value    : CoapOptionValue,
  number   : CoapOptionNumber, // questionable TODO: remove
  offset   : CoapOptionOffset  // same
)

final case class CoapOptionOffset (value: Int) extends AnyVal {
  def +(that: CoapOptionOffset): CoapOptionOffset = CoapOptionOffset(value + that.value)
}

final case class CoapOptionDelta private(value: Int) extends AnyVal
object CoapOptionDelta {
  def apply(value: Int): Either[CoapMessageException, CoapOptionDelta] =
  // #rfc7252 accepts a 4-bit unsigned integer - 15 is reserved for the payload marker
  // ... while 13 and 14 lead to special constructs via ext8 and ext16
    Either.cond(0 to 15 contains value, new CoapOptionDelta(value), InvalidOptionDelta(s"$value is not valid."))
}

final case class CoapOptionExtendedDelta private(value: Int)
object CoapOptionExtendedDelta {
  def apply(value: Int): Either[CoapMessageException, CoapOptionExtendedDelta] = {
    // #rfc7252 accepts either 8 or 16 bytes as an extension to the small delta value.
    // The extension value must be greater than 12 which is a highest non special construct value.
    Either.cond(13 to 65804 contains value, new CoapOptionExtendedDelta(value), InvalidOptionDelta(s"$value is not valid."))
  }
}

final case class CoapOptionLength private(value: Int) extends AnyVal
object CoapOptionLength {
  def apply(value: Int): Either[CoapMessageException, CoapOptionLength] =
  // #rfc7252 accepts a 4-bit unsigned integer - 15 is reserved for the payload marker
  // ... while 13 and 14 lead to special constructs via ext8 and ext16
    Either.cond(0 to 15 contains value, new CoapOptionLength(value), InvalidOptionLength(s"$value is not valid."))
}

final case class CoapOptionExtendedLength private(value: Int)
object CoapOptionExtendedLength {
  def apply(value: Int): Either[CoapMessageException, CoapOptionExtendedLength] =
  // #rfc7252 accepts either 8 or 16 bytes as an extension to the small length value.
  // The extension value must be greater than 12 which is a highest non special construct value.
    Either.cond(13 to 65804 contains value, new CoapOptionExtendedLength(value), InvalidOptionLength(s"$value is not valid."))
}