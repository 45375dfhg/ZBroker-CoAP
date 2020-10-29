package domain.model.coap.header

import domain.model.exception._
import io.estatico.newtype.macros._
import io.estatico.newtype.ops._
import utility.classExtension.ChunkExtension._
import zio._

package object fields {

  @newtype class CoapVersion private(val value: Int) {
    def toHeaderPart: Int = value << 6
  }

  object CoapVersion {
    def apply(value: Int): IO[MessageFormatError, CoapVersion] =
      // #rfc7252 knows only one valid protocol version
      IO.cond(1 to 1 contains value, value.coerce, InvalidCoapVersion(s"$value"))

    def fromByte(b: Byte): IO[MessageFormatError, CoapVersion] =
      CoapVersion((b & 0xC0) >>> 6)

    val default: CoapVersion = 1.coerce
  }

  @newtype class CoapType private(val value: Int) {
    def toHeaderPart: Int = value << 4
  }

  object CoapType {
    def apply(value: Int): IO[MessageFormatError, CoapType] =
      // #rfc7252 accepts 4 different types in a 2-bit window
      IO.cond(0 to 3 contains value, value.coerce, InvalidCoapType(s"$value"))

    def fromByte(b: Byte): IO[MessageFormatError, CoapType] =
      CoapType((b & 0x30) >> 4)

    val acknowledge: CoapType = 2.coerce
    val reset: CoapType       = 3.coerce
  }

  @newtype class CoapTokenLength private(val value: Int) {
    def toHeaderPart: Int = value
  }

  object CoapTokenLength {
    def apply(value: Int): IO[MessageFormatError, CoapTokenLength] =
      // #rfc7252 accepts a length of 0 to 8 in a 4-bit window, 9 to 15 are reserved
      IO.cond(0 to 8 contains value, value.coerce, InvalidCoapTokenLength(s"$value"))

    def fromByte(b: Byte): IO[MessageFormatError, CoapTokenLength] =
      CoapTokenLength(b & 0x0F)

    val empty: CoapTokenLength = 0.coerce
  }

  @newtype class CoapCodePrefix private(val value: Int) {
    def toHeaderPart: Int = value << 5
  }

  object CoapCodePrefix {
    def apply(value: Int): IO[MessageFormatError, CoapCodePrefix] =
      // #rfc7252 accepts prefix codes between 0 to 7 in a 3-bit window
      IO.cond(0 to 7 contains value, value.coerce, InvalidCoapCode(s"$value"))

    def fromByte(b: Byte): IO[MessageFormatError, CoapCodePrefix] =
      CoapCodePrefix((b & 0xE0) >>> 5)

    val empty: CoapCodePrefix = 0.coerce
  }

  @newtype class CoapCodeSuffix private(val value: Int) {
    def toHeaderPart: Int = value
  }

  object CoapCodeSuffix {
    def apply(value: Int): IO[MessageFormatError, CoapCodeSuffix] =
      // #rfc7252 accepts suffix codes between 0 to 31 in a 5-bit window
      IO.cond(0 to 31 contains value, value.coerce, InvalidCoapCode(s"$value"))

    def fromByte(b: Byte): IO[MessageFormatError, CoapCodeSuffix] =
      CoapCodeSuffix(b & 0x1F)

    val empty: CoapCodeSuffix = 0.coerce
  }

  @newtype class CoapId private(val value: Int) {
    def toHeaderPart: Chunk[Int] = Chunk((value >>> 8) & 0xFF, value & 0xFF)
  }

  object CoapId {
    def apply(value: Int): IO[MessageFormatError, CoapId] =
      // #rfc7252 accepts an unsigned 16-bit ID
      IO.cond(0 to 65535 contains value, value.coerce, InvalidCoapId(s"$value"))

    def fromBytes(third: Byte, fourth: Byte): IO[MessageFormatError, CoapId] =
      CoapId(((third & 0xFF) << 8) | (fourth & 0xFF))

    def recoverFrom(datagram: Chunk[Byte]): UIO[Option[CoapId]] =
      datagram.dropExactly(2).flatMap(_.takeExactly(2)).flatMap(c => fromBytes(c(0), c(1))).option

  }

}
