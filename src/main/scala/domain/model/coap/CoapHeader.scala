package domain.model.coap

import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._

import header._

final case class CoapHeader(
  version : CoapVersion,
  msgType : CoapType,
  tLength : CoapTokenLength,
  cPrefix : CoapCodePrefix,
  cSuffix : CoapCodeSuffix,
  msgID   : CoapId
)

package object header {

  @newtype class CoapVersion private(val value: Int)

  object CoapVersion {
    def apply(value: Int): Either[MessageFormatError, CoapVersion] =
      // #rfc7252 knows only one valid protocol version
      Either.cond(1 to 1 contains value, value.coerce, InvalidCoapVersion(s"$value"))

    val default: CoapVersion = 1.coerce
  }

  @newtype class CoapType private(val value: Int)

  object CoapType {
    def apply(value: Int): Either[MessageFormatError, CoapType] =
      // #rfc7252 accepts 4 different types in a 2-bit window
      Either.cond(0 to 3 contains value, value.coerce, InvalidCoapType(s"$value"))

    val empty: CoapType       = 3.coerce
    val acknowledge: CoapType = 2.coerce
  }

  @newtype class CoapTokenLength private(val value: Int)

  object CoapTokenLength {
    def apply(value: Int): Either[MessageFormatError, CoapTokenLength] =
      // #rfc7252 accepts a length of 0 to 8 in a 4-bit window, 9 to 15 are reserved
      Either.cond(0 to 8 contains value, value.coerce, InvalidCoapTokenLength(s"$value"))

    val empty: CoapTokenLength = 0.coerce
  }

  @newtype class CoapCodePrefix private(val value: Int)

  object CoapCodePrefix {
    def apply(value: Int): Either[MessageFormatError, CoapCodePrefix] =
      // #rfc7252 accepts prefix codes between 0 to 7 in a 3-bit window
      Either.cond(0 to 7 contains value, value.coerce, InvalidCoapCode(s"$value"))

    val empty: CoapCodePrefix = 0.coerce
  }

  @newtype class CoapCodeSuffix private(val value: Int)

  object CoapCodeSuffix {
    def apply(value: Int): Either[MessageFormatError, CoapCodeSuffix] =
      // #rfc7252 accepts suffix codes between 0 to 31 in a 5-bit window
      Either.cond(0 to 31 contains value, value.coerce, InvalidCoapCode(s"$value"))

    val empty: CoapCodeSuffix = 0.coerce
  }

  @newtype class CoapId private(val value: Int)

  object CoapId {
    def apply(value: Int): Either[MessageFormatError, CoapId] =
      // #rfc7252 accepts an unsigned 16-bit ID
      Either.cond(0 to 65535 contains value, value.coerce, InvalidCoapId(s"$value"))
  }
}