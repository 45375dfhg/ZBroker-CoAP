package domain.model.coap

import java.io.IOException

import zio.Chunk

final case class CoapMessage(header: CoapHeader, body: CoapBody)

final case class CoapHeader(
  version : CoapVersion,
  msgType : CoapType,
  tLength : CoapTokenLength,
  cPrefix : CoapCodePrefix,
  cSuffix : CoapCodeSuffix,
  msgID   : CoapId
)

final case class CoapBody(
  token   : Option[CoapToken],
  options : Option[List[CoapOption]],
  payload : Option[CoapPayload]
)

sealed trait CoapMessageException                              extends IOException

final case class InvalidCoapVersionException (err: String)     extends CoapMessageException
final case class InvalidCoapTypeException (err: String)        extends CoapMessageException
final case class InvalidCoapTokenLengthException (err: String) extends CoapMessageException
final case class InvalidCoapCodeException (err: String)        extends CoapMessageException
final case class InvalidCoapIdException (err: String)          extends CoapMessageException
final case class InvalidOptionDelta (err: String)              extends CoapMessageException
final case class InvalidOptionLength (err: String)             extends CoapMessageException
final case class InvalidOptionValue (err: String)              extends CoapMessageException
final case class InvalidOptionNumber (err: String)             extends CoapMessageException

case object InvalidPayloadMarker                               extends CoapMessageException
case object InvalidCoapChunkSize                               extends CoapMessageException

// TODO: Necessary?
sealed trait CoapHeaderParameter
sealed trait CoapBodyParameter

final case class CoapVersion private(number: Int) extends AnyVal
object CoapVersion extends CoapHeaderParameter {
  def apply(number: Int): Either[CoapMessageException, CoapVersion] =
  // #rfc7252 knows only one valid protocol version
    Either.cond(1 to 1 contains number, new CoapVersion(1), InvalidCoapVersionException(s"${number} is not valid."))
}

final case class CoapType private(number: Int) extends AnyVal
object CoapType extends CoapHeaderParameter {
  def apply(number: Int): Either[CoapMessageException, CoapType] =
  // #rfc7252 accepts 4 different types in a 2-bit window
    Either.cond(0 to 3 contains number, new CoapType(number), InvalidCoapTypeException(s"${number} is not valid."))
}

final case class CoapTokenLength private(value: Int) extends AnyVal
object CoapTokenLength extends CoapHeaderParameter {
  def apply(value: Int): Either[CoapMessageException, CoapTokenLength] =
  // #rfc7252 accepts a length of 0 to 8 in a 4-bit window, 9 to 15 are reserved
    Either.cond(0 to 8 contains value, new CoapTokenLength(value), InvalidCoapTokenLengthException(s"${value} is not valid."))
}

final case class CoapCodePrefix private(number: Int) extends AnyVal
object CoapCodePrefix extends CoapHeaderParameter {
  def apply(number: Int): Either[CoapMessageException, CoapCodePrefix] =
  // #rfc7252 accepts prefix codes between 0 to 7 in a 3-bit window
    Either.cond(0 to 7 contains number, new CoapCodePrefix(number), InvalidCoapCodeException(s"${number} is not valid."))
}
final case class CoapCodeSuffix private(number: Int) extends AnyVal
object CoapCodeSuffix extends CoapHeaderParameter {
  def apply(number: Int): Either[CoapMessageException, CoapCodeSuffix] =
  // #rfc7252 accepts suffix codes between 0 to 31 in a 5-bit window
    Either.cond(0 to 31 contains number, new CoapCodeSuffix(number), InvalidCoapCodeException(s"${number} is not valid."))
}

final case class CoapId private(value: Int) extends AnyVal
object CoapId extends CoapHeaderParameter {
  def apply(value: Int): Either[CoapMessageException, CoapId] =
  // #rfc7252 accepts an unsigned 16-bit ID
    Either.cond(0 to 65535 contains value, new CoapId(value), InvalidCoapIdException(s"${value} is not valid."))
}

final case class CoapToken(value: Chunk[Byte]) extends CoapBodyParameter

final case class CoapOption(
  delta    : CoapOptionDelta,  // internal?
  exDelta  : Option[CoapExtendedDelta],
  length   : CoapOptionLength, // internal!
  exLength : Option[CoapExtendedLength],
  value    : CoapOptionValue,
  number   : CoapOptionNumber, // questionable
  offset   : CoapOptionOffset  // same
) extends CoapBodyParameter

final case class CoapOptionDelta private(value: Int) extends AnyVal
object CoapOptionDelta extends CoapBodyParameter {
  def apply(value: Int): Either[CoapMessageException, CoapOptionDelta] =
    // #rfc7252 accepts a 4-bit unsigned integer - 15 is reserved for the payload marker
    // ... while 13 and 14 lead to special constructs via ext8 and ext16
    Either.cond(0 to 15 contains value, new CoapOptionDelta(value), InvalidOptionDelta(s"${value} is not valid."))
}

final case class CoapExtendedDelta private(value: Int)
object CoapExtendedDelta extends CoapBodyParameter {
  def apply(value: Int): Either[CoapMessageException, CoapExtendedDelta] = {
    // #rfc7252 accepts either 8 or 16 bytes as an extension to the small delta value.
    // The extension value must be greater than 12 which is a highest non special construct value.
    Either.cond(13 to 65804 contains value, new CoapExtendedDelta(value), InvalidOptionDelta(s"${value} is not valid."))
  }
}

final case class CoapOptionLength private(value: Int) extends AnyVal
object CoapOptionLength extends CoapBodyParameter {
  def apply(value: Int): Either[CoapMessageException, CoapOptionLength] =
    // #rfc7252 accepts a 4-bit unsigned integer - 15 is reserved for the payload marker
    // ... while 13 and 14 lead to special constructs via ext8 and ext16
    Either.cond(0 to 15 contains value, new CoapOptionLength(value), InvalidOptionLength(s"${value} is not valid."))
}

final case class CoapExtendedLength private(value: Int)
object CoapExtendedLength extends CoapBodyParameter {
  def apply(value: Int): Either[CoapMessageException, CoapExtendedLength] =
    // #rfc7252 accepts either 8 or 16 bytes as an extension to the small length value.
    // The extension value must be greater than 12 which is a highest non special construct value.
    Either.cond(13 to 65804 contains value, new CoapExtendedLength(value), InvalidOptionLength(s"${value} is not valid."))
}

// TODO: Rename?
final case class CoapOptionOffset (value: Int) extends AnyVal {
  def +(that: CoapOptionOffset): CoapOptionOffset = CoapOptionOffset(value + that.value)
}

final case class CoapOptionValue(value: Chunk[Byte]) extends CoapBodyParameter // TODO: Implementation

final case class CoapOptionNumber(value: Int) extends AnyVal

final case class CoapPayload(value: Chunk[Byte]) extends CoapBodyParameter


