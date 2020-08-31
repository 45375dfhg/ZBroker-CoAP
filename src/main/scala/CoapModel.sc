import java.io.IOException

import zio.Chunk

import scala.annotation.tailrec

final case class CoapMessage(header: CoapHeader)

final case class CoapHeader(pVersion: CoapVersion,
                            mType: CoapType,
                            tLength: CoapTokenLength,
                            mPref: CodePrefix,
                            mSuf: CodeSuffix,
                            mId: CoapId)


// raw head => head => raw body => body => message

//final case class CoapBody(tValue: CoapToken,
//                          options: CoapOptions,
//                          payload: CoapPayload)

case object EmptyMessageException extends CoapMessageException

sealed trait Coap {
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

case object CoapConversionException extends CoapMessageException

case object CoapHeader extends Coap {
  def fromChunk(data: Chunk[Boolean]): Either[CoapMessageException, CoapHeader] = {
    @tailrec
    def parseBitsByOffsets(
                            rem: Chunk[Boolean],
                            acc: List[Either[CoapMessageException, (String, Int)]] = List.empty,
                            offsets: List[(String, Int)] = headerBits): Either[CoapMessageException, Map[String, Int]] = {
      offsets.headOption match {
        case Some((key, bitsN)) => parse(rem.take(bitsN)) match {
          case r @ Right(_) => parseBitsByOffsets(rem.drop(bitsN), r.map((key, _)) :: acc, offsets.tail)
          case Left(_)      => Left(CoapConversionException)
        }
        case None               => if (acc.nonEmpty) acc.partitionMap(identity) match {
          case (Nil, rights) => Right(rights.toMap)
          case (lefts, _)    => Left(lefts.head)
        } else Left(CoapConversionException)

      }
    }
    def constructHeaderFromInt(m: Map[String, Int]): Either[CoapMessageException, CoapHeader] = {
      def getAsEither(s: String): Either[CoapConversionException.type, Int] = m.get(s).toRight(CoapConversionException)

      for {
        version    <- getAsEither("version").flatMap(CoapVersion(_))
        msgType    <- getAsEither("type").flatMap(CoapType(_))
        tLength    <- getAsEither("length").flatMap(CoapTokenLength(_))
        msgPreCode <- getAsEither("prefix").flatMap(CodePrefix(_))
        msgSufCode <- getAsEither("suffix").flatMap(CodeSuffix(_))
        msgId      <- getAsEither("id").flatMap(CoapId(_))
        // msgCode    <- Right(CoapCode(msgPreCode, msgSufCode))
      } yield CoapHeader(version, msgType, tLength, msgPreCode, msgSufCode, msgId)
    }

    parseBitsByOffsets(data).flatMap(constructHeaderFromInt)
  }

  private val headerBits = List(("version", 2), ("type", 2), ("length", 4), ("prefix", 3), ("suffix", 5), ("id", 16))
}

sealed trait CoapMessageException extends IOException

sealed trait CoapHeaderParameter

class CoapVersion private(val number: Int) extends AnyVal
case object InvalidCoapVersionException extends CoapMessageException
object CoapVersion extends CoapHeaderParameter {
  def apply(number: Int): Either[CoapMessageException, CoapVersion] =
    // #rfc7252 knows only one valid protocol version
    Either.cond(1 to 1 contains number, new CoapVersion(1), InvalidCoapVersionException)
}

class CoapType private(val number: Int) extends AnyVal {}
case object InvalidCoapTypeException extends CoapMessageException
object CoapType extends CoapHeaderParameter {
  def apply(number: Int): Either[CoapMessageException, CoapType] =
    // #rfc7252 accepts 4 different types in a 2-bit window
    Either.cond(0 to 3 contains number, new CoapType(number), InvalidCoapTypeException)
}

class CoapTokenLength private(val value: Int) extends AnyVal {}
case object InvalidCoapTokenLengthException extends CoapMessageException
object CoapTokenLength extends CoapHeaderParameter {
  def apply(value: Int): Either[CoapMessageException, CoapTokenLength] =
    // #rfc7252 accepts a length of 0 to 8 in a 4-bit window, 9 to 15 are reserved
    Either.cond(0 to 8 contains value, new CoapTokenLength(value), InvalidCoapTokenLengthException)
}

final case class CoapCode(prefix: CodePrefix, suffix: CodeSuffix)
case object InvalidCoapCodeException extends CoapMessageException

class CodePrefix private(val number: Int) extends AnyVal {}
object CodePrefix extends CoapHeaderParameter {
  def apply(number: Int): Either[CoapMessageException, CodePrefix] =
    // #rfc7252 accepts prefix codes between 0 to 7 in a 3-bit window
    Either.cond(0 to 7 contains number, new CodePrefix(number), InvalidCoapCodeException)
}
class CodeSuffix private(val number: Int) extends AnyVal {}
object CodeSuffix extends CoapHeaderParameter {
  def apply(number: Int): Either[CoapMessageException, CodeSuffix] =
    // #rfc7252 accepts suffix codes between 0 to 31 in a 5-bit window
    Either.cond(0 to 31 contains number, new CodeSuffix(number), InvalidCoapCodeException)
}

class CoapId private(val value: Int) extends AnyVal {}
case object InvalidCoapIdException extends CoapMessageException
object CoapId extends CoapHeaderParameter {
  def apply(value: Int): Either[CoapMessageException, CoapId] =
    // #rfc7252 accepts an unsigned 16-bit ID
    Either.cond(0 to 65535 contains value, new CoapId(value), InvalidCoapIdException)
}

final case class RawBody(data: Chunk[Boolean], tLength: CoapTokenLength) { self =>
  def toCoapBody = ???

}

CoapHeader.fromChunk(Chunk(true))
// raw header and body useless
// instead CoapHeader.fromChunk() & CoapBody.fromChunk

// 1. extract token if necessary
// 2. skim rest for possible option ends
// 3. check for eof / payload marker
// marker only exists if payload is non-zero
// ! marker value can be inside option length etc so scanning not recommended
// => parallel processing very limited

// Token, Options, Payload

// 1. receive chunk
// 2. split chunk into header and body | fail if header too small
// 3. read raw header => coap header
// 4. use coap header values to read raw body => coap body
// 5. return both

// translation layer for mappings?












