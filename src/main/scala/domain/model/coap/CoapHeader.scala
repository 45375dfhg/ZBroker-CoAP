package domain.model.coap

/*
 * The entry point is in {{{CoapMessage}}}
 */

final case class CoapHeader(
  version : CoapVersion,
  msgType : CoapType,
  tLength : CoapTokenLength,
  cPrefix : CoapCodePrefix,
  cSuffix : CoapCodeSuffix,
  msgID   : CoapId
)

final case class CoapVersion private(number: Int) extends AnyVal
object CoapVersion {
  def apply(number: Int): Either[InvalidCoapMessage, CoapVersion] =
  // #rfc7252 knows only one valid protocol version
    Either.cond(1 to 1 contains number, new CoapVersion(1), InvalidCoapVersionException(s"$number is not valid."))
}

final case class CoapType private(number: Int) extends AnyVal
object CoapType {
  def apply(number: Int): Either[InvalidCoapMessage, CoapType] =
  // #rfc7252 accepts 4 different types in a 2-bit window
    Either.cond(0 to 3 contains number, new CoapType(number), InvalidCoapTypeException(s"$number is not valid."))
}

final case class CoapTokenLength private(value: Int) extends AnyVal
object CoapTokenLength {
  def apply(value: Int): Either[InvalidCoapMessage, CoapTokenLength] =
  // #rfc7252 accepts a length of 0 to 8 in a 4-bit window, 9 to 15 are reserved
    Either.cond(0 to 8 contains value, new CoapTokenLength(value), InvalidCoapTokenLengthException(s"$value is not valid."))
}

final case class CoapCodePrefix private(number: Int) extends AnyVal
object CoapCodePrefix {
  def apply(number: Int): Either[InvalidCoapMessage, CoapCodePrefix] =
  // #rfc7252 accepts prefix codes between 0 to 7 in a 3-bit window
    Either.cond(0 to 7 contains number, new CoapCodePrefix(number), InvalidCoapCodeException(s"$number is not valid."))
}
final case class CoapCodeSuffix private(number: Int) extends AnyVal
object CoapCodeSuffix {
  def apply(number: Int): Either[InvalidCoapMessage, CoapCodeSuffix] =
  // #rfc7252 accepts suffix codes between 0 to 31 in a 5-bit window
    Either.cond(0 to 31 contains number, new CoapCodeSuffix(number), InvalidCoapCodeException(s"$number is not valid."))
}

final case class CoapId private(value: Int) extends AnyVal
object CoapId {
  def apply(value: Int): Either[InvalidCoapMessage, CoapId] =
  // #rfc7252 accepts an unsigned 16-bit ID
    Either.cond(0 to 65535 contains value, new CoapId(value), InvalidCoapIdException(s"$value is not valid."))
}