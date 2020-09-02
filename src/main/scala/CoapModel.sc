//import java.io.IOException
//
//import zio.Chunk
//
//import scala.annotation.tailrec
//
//final case class CoapMessage(header: CoapHeader)
//
//final case class CoapHeader(pVersion: CoapVersion,
//                            mType: CoapType,
//                            tLength: CoapTokenLength,
//                            mPref: CodePrefix,
//                            mSuf: CodeSuffix,
//                            mId: CoapId)
//
//
//// raw head => head => raw body => body => message
//
//case object EmptyMessageException extends CoapMessageException
//
//sealed trait Coap {
//  def parse(in: Chunk[Boolean]): Either[CoapMessageException, Int] = {
//    @tailrec
//    def loop(rem: Chunk[Boolean], acc: Int = 0, power: Int = 0): Either[CoapMessageException, Int] =
//      rem.lastOption match {
//        case Some(bool) => if (bool) loop(rem.dropRight(1), acc + (1 << power), power + 1)
//                           else loop(rem.dropRight(1), acc, power + 1)
//        case None       => if (power == 0) Left(EmptyMessageException) else Right(acc)
//      }
//    loop(in)
//  }
//}
//
//case object CoapConversionException extends CoapMessageException
//
//// TODO: Rewrite so errors get accumulated and no error short circuiting
//// TODO: Rewrite - right now take() can take less than bitsN elements without failing
//case object CoapHeader extends Coap {
//  def fromChunk(data: Chunk[Boolean]): Either[CoapMessageException, CoapHeader] = {
//    @tailrec
//    def parseBitsByOffsets(
//      rem: Chunk[Boolean],
//      acc: List[Either[CoapMessageException, (String, Int)]] = List.empty,
//      offsets: List[(String, Int)] = headerBits
//    ): Either[CoapMessageException, Map[String, Int]] = {
//      offsets.headOption match {
//        case Some((key, bitsN)) => parse(rem.take(bitsN)) match {
//          case r @ Right(_) => parseBitsByOffsets(rem.drop(bitsN), r.map((key, _)) :: acc, offsets.tail)
//          case Left(_)      => Left(CoapConversionException)
//        }
//        case None               => if (acc.nonEmpty) acc.partitionMap(identity) match {
//          case (Nil, rights) => Right(rights.toMap)
//          case (lefts, _)    => Left(lefts.head)
//        } else Left(CoapConversionException)
//      }
//    }
//
//    def constructHeaderFromInt(m: Map[String, Int]): Either[CoapMessageException, CoapHeader] = {
//      def getAsEither(s: String): Either[CoapConversionException.type, Int] = m.get(s).toRight(CoapConversionException)
//
//      for {
//        version    <- getAsEither("version").flatMap(CoapVersion(_))
//        msgType    <- getAsEither("type").flatMap(CoapType(_))
//        tLength    <- getAsEither("length").flatMap(CoapTokenLength(_))
//        msgPreCode <- getAsEither("prefix").flatMap(CodePrefix(_))
//        msgSufCode <- getAsEither("suffix").flatMap(CodeSuffix(_))
//        msgId      <- getAsEither("id").flatMap(CoapId(_))
//        // msgCode    <- Right(CoapCode(msgPreCode, msgSufCode))
//      } yield CoapHeader(version, msgType, tLength, msgPreCode, msgSufCode, msgId)
//    }
//
//    parseBitsByOffsets(data).flatMap(constructHeaderFromInt)
//  }
//
//  private val headerBits = List(("version", 2), ("type", 2), ("length", 4), ("prefix", 3), ("suffix", 5), ("id", 16))
//}
//
//sealed trait CoapMessageException extends IOException
//
//sealed trait CoapHeaderParameter
//
//class CoapVersion private(val number: Int) extends AnyVal
//case object InvalidCoapVersionException extends CoapMessageException
//object CoapVersion extends CoapHeaderParameter {
//  def apply(number: Int): Either[CoapMessageException, CoapVersion] =
//    // #rfc7252 knows only one valid protocol version
//    Either.cond(1 to 1 contains number, new CoapVersion(1), InvalidCoapVersionException)
//}
//
//class CoapType private(val number: Int) extends AnyVal {}
//case object InvalidCoapTypeException extends CoapMessageException
//object CoapType extends CoapHeaderParameter {
//  def apply(number: Int): Either[CoapMessageException, CoapType] =
//    // #rfc7252 accepts 4 different types in a 2-bit window
//    Either.cond(0 to 3 contains number, new CoapType(number), InvalidCoapTypeException)
//}
//
//class CoapTokenLength private(val value: Int) extends AnyVal {}
//case object InvalidCoapTokenLengthException extends CoapMessageException
//object CoapTokenLength extends CoapHeaderParameter {
//  def apply(value: Int): Either[CoapMessageException, CoapTokenLength] =
//    // #rfc7252 accepts a length of 0 to 8 in a 4-bit window, 9 to 15 are reserved
//    Either.cond(0 to 8 contains value, new CoapTokenLength(value), InvalidCoapTokenLengthException)
//}
//
//final case class CoapCode(prefix: CodePrefix, suffix: CodeSuffix)
//case object InvalidCoapCodeException extends CoapMessageException
//
//class CodePrefix private(val number: Int) extends AnyVal {}
//object CodePrefix extends CoapHeaderParameter {
//  def apply(number: Int): Either[CoapMessageException, CodePrefix] =
//    // #rfc7252 accepts prefix codes between 0 to 7 in a 3-bit window
//    Either.cond(0 to 7 contains number, new CodePrefix(number), InvalidCoapCodeException)
//}
//class CodeSuffix private(val number: Int) extends AnyVal {}
//object CodeSuffix extends CoapHeaderParameter {
//  def apply(number: Int): Either[CoapMessageException, CodeSuffix] =
//    // #rfc7252 accepts suffix codes between 0 to 31 in a 5-bit window
//    Either.cond(0 to 31 contains number, new CodeSuffix(number), InvalidCoapCodeException)
//}
//
//class CoapId private(val value: Int) extends AnyVal {}
//case object InvalidCoapIdException extends CoapMessageException
//object CoapId extends CoapHeaderParameter {
//  def apply(value: Int): Either[CoapMessageException, CoapId] =
//    // #rfc7252 accepts an unsigned 16-bit ID
//    Either.cond(0 to 65535 contains value, new CoapId(value), InvalidCoapIdException)
//}
//
//case object InvalidOptionDelta extends CoapMessageException
//
////class OptionDeltaNorm private(val value: Int) extends AnyVal {}
////class OptionDeltaExt8 private(val value: Int) extends AnyVal {}
////class OptionDeltaExt16 private(val value: Int) extends AnyVal {}
//
//sealed trait OptionDelta
//final case class OptionDeltaNorm(value: Int) extends OptionDelta
//final case class OptionDeltaExt8(value: Int) extends OptionDelta
//final case class OptionDeltaExt16(value: Int) extends OptionDelta
//
//object OptionDeltaNorm {
//  def apply(value: Int): Either[CoapMessageException, OptionDelta] =
//    // #rfc7252 accepts a 4-bit unsigned integer - 15 is reserved for the payload marker
//    // ... while 13 and 14 lead to special constructs via ext8 and ext16
//    Either.cond(0 to 14 contains value, new OptionDeltaNorm(value), InvalidOptionDelta)
//}
//object OptionDeltaExt8 {
//  def apply(value: Int): Either[CoapMessageException, OptionDelta] =
//    // #rfc7252 maps 13 to a new byte which represents the delta minus 13
//    Either.cond(13 to 268 contains value, new OptionDeltaExt8(value), InvalidOptionDelta)
//}
//
//object OptionDeltaExt16 {
//  def apply(value: Int): Either[CoapMessageException, OptionDelta] =
//    // #rfc7252 maps 14 to two bytes which represents the delta minus 269
//    Either.cond(269 to 65804 contains value, new OptionDeltaExt16(value), InvalidOptionDelta)
//}
//
//class OptionLength private(val value: Int) extends AnyVal {}
//case object InvalidOptionLength extends CoapMessageException
//object OptionLength {
//  def apply(value: Int): Either[CoapMessageException, OptionLength] =
//    // #rfc7252 accepts a 4-bit unsigned integer - 15 is reserved for future use, must be processed as format error
//    // ... while 13 and 14 lead to special constructs via ext8 and ext16
//    Either.cond(0 to 14 contains value, new OptionLength(value), InvalidOptionLength)
//
//  def ext8(value: Int): Either[CoapMessageException, OptionLength] =
//    // #rfc7252 maps 13 to a new byte which represents the length minus 13
//    Either.cond(13 to 268 contains value, new OptionLength(value), InvalidOptionLength)
//
//  def ext16(value: Int): Either[CoapMessageException, OptionLength] =
//    // #rfc7252 maps 14 to two bytes which represents the length minus 269
//    Either.cond(269 to 65804 contains value, new OptionLength(value), InvalidOptionLength)
//}
//
//
//
//// TODO: Refactor - is the extension required? what about empty and unchanged?
//final case class OptionValue(value: Chunk[Boolean]) extends Coap { self =>
//  def asEmpty: Unit = ???
//  def asString = ??? // bit shifting :)
//  def asUnsignedInt = ??? // parsed
//  def unchanged = self
//}
//
//final case class CoapBody(token: CoapToken,
//                          options: List[CoapOption],
//                          payload: CoapPayload)
//
//final case class CoapOption(delta: OptionDelta, length: OptionLength, value: OptionValue)
//
//// TODO: Refactor
//class CoapToken (val value: Int) extends AnyVal {}
//class CoapPayload (val value: Int) extends AnyVal {}
//
//case object CoapBody extends Coap {
//  def fromChunkAndHeader(chunk: Chunk[Boolean])(header: CoapHeader) = {
//    // utility functions to work with or around the token
//    def extractToken: Chunk[Boolean] = chunk.take(header.tLength.value) // TODO: Parsing?!
//    def dropToken: Chunk[Boolean]    = chunk.drop(header.tLength.value)
//
//    def detectPayloadMarker(marker: Chunk[Boolean]) = marker.forall(_ == true)
//
//
//    def parseOptions(rem: Chunk[Boolean], acc: List[CoapOption] = List.empty, num: Int = 0) = {
//      if (rem.lengthCompare(8) >= 0) {
//        val (delta, length) = (parse(rem.take(4)), rem.takeRight(4))
//
//        val extDelta = delta.flatMap {
//          case 13    => parse(rem.slice(8, 16)).flatMap(OptionDeltaExt8(_))
//          case 14    => parse(rem.slice(8, 32)).flatMap(OptionDeltaExt16(_))
//          case other => OptionDeltaNorm(other)
//        }
//
//        val extLength = length.flatMap {
//          case 13    => parse(rem.slice)
//          case 14    =>
//          case other =>
//        }
//
//
//
////        val extLength = length.flatMap {
////          case 13    => parse(rem.slice(8, 16)).flatMap(OptionDelta.ext8)
////          case 14    => parse(rem.slice(8, 32)).flatMap(OptionDelta.ext16)
////          case other => OptionLength(other)
////        }
//      }
//    }
//
//    // NON LOOP
//    // Extract Token
//
//    // LOOP
//    // 1. detect payload marker -> if found end parse payload end loop
//    // 2. Check Delta and Length for > 12 branch and read all three parts depending on results
//    // ! Option Parsing is the "true" loop
//  }
//}
//
//import zio._
//import zio.stream._
//
//val k = Chunk(true, false)
//val t = CoapHeader.fromChunk(k)
//val r = ZStream(k).mapM(chunk => ZIO.fromEither(CoapHeader.fromChunk(chunk)))
//
//// 1. extract token if necessary
//// 2. check for eof then payload then option else error
//// marker only exists if payload is non-zero
//
//// Read Option
//// 1  Check Delta for > 12
//// 2  Check Length for > 12
//// 3  if (1) read 8 or 16 bits after first byte
//// 4  if (2) read 0 or 8 or 16 bits (depended on 1) after first byte
//// 5  read 4 bits after 0 or 8 or 16 or 24 or 32 bits after first byte
//
//
//// Token, Options, Payload
//
//// 1. receive chunk
//// 2. split chunk into header and body | fail if header too small
//// 3. read raw header => coap header
//// 4. use coap header values to read raw body => coap body
//// 5. return both
//
//// translation layer for mappings?
//
//
//
//
//
//
//
//
//
//
//
//
