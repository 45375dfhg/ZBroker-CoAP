import java.io.IOException

import zio.Chunk

final case class CoapMessage[T](header: CoapHeader, body: CoapBody[T])

final case class CoapHeader(
  version : CoapVersion,
  msgType : CoapType,
  tLength : CoapTokenLength,
  cPrefix : CoapCodePrefix,
  cSuffix : CoapCodeSuffix,
  msgID   : CoapId
)

final case class CoapBody[T](
  token   : CoapToken,
  options : List[CoapOption],
  payload : CoapPayload[T]
)

sealed trait CoapMessageException           extends IOException

case object InvalidCoapVersionException     extends CoapMessageException
case object InvalidCoapTypeException        extends CoapMessageException
case object InvalidCoapTokenLengthException extends CoapMessageException
case object InvalidCoapCodeException        extends CoapMessageException
case object InvalidCoapIdException          extends CoapMessageException

// TODO: Necessary?
sealed trait CoapHeaderParameter
sealed trait CoapBodyParameter

final case class CoapVersion private(number: Int) extends AnyVal
object CoapVersion extends CoapHeaderParameter {
  def apply(number: Int): Either[CoapMessageException, CoapVersion] =
    // #rfc7252 knows only one valid protocol version
    Either.cond(1 to 1 contains number, new CoapVersion(1), InvalidCoapVersionException)
}

final case class CoapType private(number: Int) extends AnyVal
object CoapType extends CoapHeaderParameter {
  def apply(number: Int): Either[CoapMessageException, CoapType] =
    // #rfc7252 accepts 4 different types in a 2-bit window
    Either.cond(0 to 3 contains number, new CoapType(number), InvalidCoapTypeException)
}

final case class CoapTokenLength private(value: Int) extends AnyVal
object CoapTokenLength extends CoapHeaderParameter {
  def apply(value: Int): Either[CoapMessageException, CoapTokenLength] =
    // #rfc7252 accepts a length of 0 to 8 in a 4-bit window, 9 to 15 are reserved
    Either.cond(0 to 8 contains value, new CoapTokenLength(value), InvalidCoapTokenLengthException)
}

final case class CoapCodePrefix private(number: Int) extends AnyVal
object CoapCodePrefix extends CoapHeaderParameter {
  def apply(number: Int): Either[CoapMessageException, CoapCodePrefix] =
    // #rfc7252 accepts prefix codes between 0 to 7 in a 3-bit window
    Either.cond(0 to 7 contains number, new CoapCodePrefix(number), InvalidCoapCodeException)
}
final case class CoapCodeSuffix private(number: Int) extends AnyVal
object CoapCodeSuffix extends CoapHeaderParameter {
  def apply(number: Int): Either[CoapMessageException, CoapCodeSuffix] =
    // #rfc7252 accepts suffix codes between 0 to 31 in a 5-bit window
    Either.cond(0 to 31 contains number, new CoapCodeSuffix(number), InvalidCoapCodeException)
}

final case class CoapId private(value: Int) extends AnyVal
object CoapId extends CoapHeaderParameter {
  def apply(value: Int): Either[CoapMessageException, CoapId] =
    // #rfc7252 accepts an unsigned 16-bit ID
    Either.cond(0 to 65535 contains value, new CoapId(value), InvalidCoapIdException)
}

case object InvalidOptionDelta  extends CoapMessageException
case object InvalidOptionLength extends CoapMessageException
case object InvalidOptionValue  extends CoapMessageException
case object InvalidOptionNumber extends CoapMessageException

final case class CoapToken(byte: Chunk[Byte]) extends CoapBodyParameter

final case class CoapOption(
  delta  : CoapOptionDelta,  // internal?
  length : CoapOptionLength, // internal!
  value  : CoapOptionValue,
  number : CoapOptionNumber
) extends CoapBodyParameter

final case class CoapOptionDelta(value: Int) extends AnyVal
object CoapOptionDelta extends CoapBodyParameter {
  def apply(value: Int): Either[CoapMessageException, CoapOptionDelta] =
    // #rfc7252 accepts a 4-bit unsigned integer - 15 is reserved for the payload marker
    // ... while 13 and 14 lead to special constructs via ext8 and ext16
    Either.cond(0 to 65804 contains value, new CoapOptionDelta(value), InvalidOptionDelta)
}

final case class CoapOptionLength(value: Int) extends AnyVal
object CoapOptionLength extends CoapBodyParameter {
  def apply(value: Int): Either[CoapMessageException, CoapOptionLength] =
    // #rfc7252 accepts a 4-bit unsigned integer - 15 is reserved for the payload marker
    // ... while 13 and 14 lead to special constructs via ext8 and ext16
    Either.cond(0 to 65804 contains value, new CoapOptionLength(value), InvalidOptionLength)
}

final case class CoapOptionValue() // TODO: Implementation

final case class CoapOptionNumber(value: Int) extends AnyVal

sealed abstract class CoapPayLoad
final case class CoapPayload[T](value: T) extends CoapBodyParameter
