import java.io.IOException

sealed trait Message
final case class CoapMessage(header: CoapHeader,
                            // body: CoapBody
                            )

final case class CoapHeader(pVersion: CoapVersion,
                            mType: CoapType,
                            tLength: CoapTokenLength,
                            mCode: CoapCode,
                            mId: CoapId)

sealed trait CoapMessageException extends IOException

class CoapVersion private(val number: Int) extends AnyVal {}
case object InvalidCoapVersionException extends CoapMessageException
object CoapVersion {
  def apply(number: Int): Either[CoapMessageException, CoapVersion] =
    // #rfc7252 knows only one valid protocol version
    Either.cond(1 to 1 contains number, new CoapVersion(1), InvalidCoapVersionException)
}

class CoapType private(val number: Int) extends AnyVal {}
case object InvalidCoapTypeException extends CoapMessageException
object CoapType {
  def apply(number: Int): Either[CoapMessageException, CoapType] =
    // #rfc7252 accepts 4 different types in a 2-bit window
    Either.cond(0 to 3 contains number, new CoapType(number), InvalidCoapTypeException)
}

class CoapTokenLength private(val value: Int) extends AnyVal {}
case object InvalidCoapTokenLengthException extends CoapMessageException
object CoapTokenLength {
  def apply(value: Int): Either[CoapMessageException, CoapTokenLength] =
    // #rfc7252 accepts a length of 0 to 8 in a 4-bit window, 9 to 15 are reserved
    Either.cond(0 to 8 contains value, new CoapTokenLength(value), InvalidCoapTokenLengthException)
}

final case class CoapCode(prefix: CodePrefix, suffix: CodeSuffix)
case object InvalidCoapCodeException extends CoapMessageException

class CodePrefix private(val number: Int) extends AnyVal {}
object CodePrefix {
  def apply(number: Int): Either[CoapMessageException, CodePrefix] =
    // #rfc7252 accepts prefix codes between 0 to 7 in a 3-bit window
    Either.cond(0 to 7 contains number, new CodePrefix(number), InvalidCoapCodeException)
}
class CodeSuffix private(val number: Int) extends AnyVal {}
object CodeSuffix {
  def apply(number: Int): Either[CoapMessageException, CodeSuffix] =
    // #rfc7252 accepts suffix codes between 0 to 31 in a 5-bit window
    Either.cond(0 to 31 contains number, new CodeSuffix(number), InvalidCoapCodeException)
}

// 65536
class CoapId private(val value: Int) extends AnyVal {}
case object InvalidCoapIdException extends CoapMessageException
object CoapId {
  def apply(value: Int): Either[CoapMessageException, CoapId] =
    Either.cond(0 to 65535 contains value, new CoapId(value), InvalidCoapIdException)
}







