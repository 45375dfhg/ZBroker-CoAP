import java.io.IOException

import zio.Chunk

import scala.annotation.tailrec

//sealed trait Message
//final case class CoapMessage(header: CoapHeader
//                            // body: CoapBody
//                            )
final case class CoapMessage(header: CoapHeader)

case object EmptyMessageException extends CoapMessageException

object Coap {
  def parse(in: Chunk[Boolean]): Either[CoapMessageException, Int] = {
    @tailrec
    def loop(rem: Chunk[Boolean], acc: Int = 0, power: Int = 0): Either[CoapMessageException, Int] =
      rem.lastOption match {
        case Some(bool) => if (bool) loop(rem.dropRight(1), acc + (1 << power), power + 1)
                           else loop(rem.dropRight(1), acc, power + 1)
        case None       => if (power == 0) Left(EmptyMessageException) else Right(acc)
      }
    loop(in)
  }
}
sealed trait RawCoap {
  def parse(in: Chunk[Boolean]): Either[CoapMessageException, Int] = {
    @tailrec
    def loop(rem: Chunk[Boolean], acc: Int = 0, power: Int = 0): Either[CoapMessageException, Int] =
      rem.lastOption match {
        case Some(bool) => if (bool) loop(rem.dropRight(1), acc + (1 << power), power + 1)
        else loop(rem.dropRight(1), acc, power + 1)
        case None       => if (power == 0) Left(EmptyMessageException) else Right(acc)
      }
    loop(in)
  }

  val headerBits: List[Int] = List(2, 2, 4, 3, 5, 16)
}

case object CoapConversionException extends CoapMessageException
final case class RawHeader(data: Chunk[Boolean]) extends RawCoap { self =>
  def toCoapHeader: Either[CoapMessageException, CoapHeader] = {
    @tailrec
    def parseBitsByOffsets(
                           rem: Chunk[Boolean],
                           acc: List[Either[CoapMessageException, Int]] = List.empty,
                           offsets: List[Int] = headerBits): Either[CoapMessageException, List[Int]] = {
      offsets.headOption match {
        case Some(bitsN) => parse(rem.take(bitsN)) match {
          case a @ Right(_) => parseBitsByOffsets(rem.drop(bitsN), a :: acc, offsets.tail)
          case Left(_)      => Left(CoapConversionException)
        }
        case None        => if (acc.nonEmpty) acc.partitionMap(identity) match {
                                case (Nil, rights) => Right(rights.reverse)
                                case (lefts, _)    => Left(lefts.head)
                              } else Left(CoapConversionException)
      }
    }
    def constructHeaderFromInt(values: List[Int]): Either[CoapMessageException, CoapHeader] =
      if (values.length == headerBits.length) {
        for {
          version    <- CoapVersion(values(0))
          msgType    <- CoapType(values(1))
          tLength    <- CoapTokenLength(values(2))
          msgPreCode <- CodePrefix(values(3))
          msgSufCode <- CodeSuffix(values(4))
          msgId      <- CoapId(values(5))
        } yield CoapHeader(version, msgType, tLength, msgPreCode, msgSufCode, msgId)
      } else Left(CoapConversionException)
    parseBitsByOffsets(data).flatMap(constructHeaderFromInt)
  }
}
final case class RawBody(data: Chunk[Boolean]) {

}

// raw header, raw body => header, raw body => header, body

sealed trait CoapMessageException extends IOException

class CoapVersion private(val number: Int) extends AnyVal
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

class CoapId private(val value: Int) extends AnyVal {}
case object InvalidCoapIdException extends CoapMessageException
object CoapId {
  def apply(value: Int): Either[CoapMessageException, CoapId] =
    // #rfc7252 accepts an unsigned 16-bit ID
    Either.cond(0 to 65535 contains value, new CoapId(value), InvalidCoapIdException)
}

final case class CoapHeader(pVersion: CoapVersion,
                            mType: CoapType,
                            tLength: CoapTokenLength,
                            mPref: CodePrefix,
                            mSuf: CodeSuffix,
                            mId: CoapId)










