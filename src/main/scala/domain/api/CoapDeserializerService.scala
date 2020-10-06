package domain.api

import domain.model.coap.{body, _}
import domain.model.coap.body.{CoapBody, CoapOption, CoapPayload, CoapPayloadMediaType, SniffingMediaType}
import domain.model.coap.header._
import domain.model.coap.option._
import domain.model.exception.{GatewayError, InvalidOptionDelta, InvalidOptionLength, InvalidPayloadMarker, MessageFormatError}

import Numeric.Implicits._
import utility.ChunkExtension._
import zio.{Chunk, _}


/**
 * This service provides the functionality to extract CoAP parameters from Chunk[Byte]
 * and transform them to a model representation of a CoAP message for internal usage.
 */
object CoapDeserializerService {

  type IgnoredMessageWithIdOption = (MessageFormatError, Option[CoapId])
  type IgnoredMessageWithId       = (MessageFormatError, CoapId)
  /**
   * Takes a chunk and attempts to convert it into a CoapMessage.
   * <p>
   * Calls via take and drop might return an empty Chunk instead of failing.
   * Therefore error handling has to be done on different layers of the transformation.
   * <p>
   * Error handling is done via short-circuiting since a malformed packet would throw
   * too many and mostly useless errors. Thus, a top-down error search is implemented.
   */

  def extractFromChunk(chunk: Chunk[Byte]): UIO[Either[IgnoredMessageWithIdOption, CoapMessage]] =
    (for {
      header  <- CoapHeader.fromDatagram(chunk)
      body    <- chunk.dropExactly(4) >>= (bodyFromChunk(_, header))
      message <- validateMessage(header, body)
    } yield message).flatMapError(err => URIO.succeed(err) <*> CoapId.fromDatagram(chunk).option).either

  /**
   * Attempts to form a CoapHeader when handed a Chunk of size 4.
   * Might fail with header parameter dependent errors.
   */
  private def headerFromChunk(chunk: Chunk[Byte]): IO[MessageFormatError, CoapHeader] = {
    // This extraction is reliant on the fact that this function will only be called after takeExactly was called!
    val (b1, b2, b3, b4) = (chunk(0), chunk(1), chunk(2), chunk(3))

    for {
      v <- CoapVersion.fromByte(b1)
      t <- CoapType.fromByte(b1)
      l <- CoapTokenLength.fromByte(b1)
      p <- CoapCodePrefix.fromByte(b2)
      s <- CoapCodeSuffix.fromByte(b2)
      i <- CoapId.fromBytes(b3, b4)
    } yield CoapHeader(v, t, l ,p, s, i)
  }

  /**
   * Extracts the token from the body and attempts to recursively read the options from the body.
   * Before each attempt there is a check for a payload marker and end of chunk.
   *
   * @param chunk can be of any size smaller than the maximum datagram size
   *              which should be filtered by the buffer anyway.
   * @param header is required to extract the respective token length.
   * @return Either a CoapBody or an Exception
   */
  private def bodyFromChunk(chunk: Chunk[Byte], header: CoapHeader): IO[MessageFormatError, CoapBody] = {

    type OptionListAndPayload = (Chunk[CoapOption], Chunk[Byte])

    type OptionTuple = (Option[NonEmptyChunk[CoapOption]], Option[CoapPayload])

    def extractTokenFrom(chunk: Chunk[Byte]): IO[GatewayError, Option[CoapToken]] = {
      if (header.coapTokenLength.value > 0) chunk.takeExactlyN(header.coapTokenLength.value).map(t => Some(CoapToken(t)))
      else ZIO.none
    }

    def getOptionListFrom2(
      chunk: Chunk[Byte], acc: Chunk[CoapOption] = Chunk.empty, num: Int = 0
    ): IO[MessageFormatError, OptionTuple] =
      chunk.headOption match {
        case Some(b) =>
          if (b == 0xFF.toByte)
            IO.fromOption(NonEmptyChunk.fromChunk(chunk.drop(1)))
              .orElseFail(InvalidPayloadMarker)
              .map(load => (NonEmptyChunk.fromChunk(acc), Some(CoapPayload.fromWithExcluding(load, )))

            // IO.cond(chunk.tail.nonEmpty, (acc, chunk.drop(1)), InvalidPayloadMarker).flatMap { case (list, load) =>

          // ZIO assures that these kind of non-tail recursive calls are stack- and heap-safe
          else getNextOption(chunk, num) >>= (o => getOptionListFrom(chunk.drop(o.offset.value), acc :+ o, o.coapOptionNumber.value))
        case None => IO.succeed(acc, Chunk.empty)
      }


    def getOptionListFrom(
      chunk: Chunk[Byte], acc: Chunk[CoapOption] = Chunk.empty, num: Int = 0
    ): IO[MessageFormatError, OptionListAndPayload] =
      chunk.headOption match {
        case Some(b) =>
          if (b == 0xFF.toByte) IO.cond(chunk.tail.nonEmpty, (acc, chunk.drop(1)), InvalidPayloadMarker)
          // ZIO assures that these kind of non-tail recursive calls are stack- and heap-safe
          else getNextOption(chunk, num) >>= (o => getOptionListFrom(chunk.drop(o.offset.value), acc :+ o, o.coapOptionNumber.value))
        case None => IO.succeed(acc, Chunk.empty)
      }

    def getNextOption(chunk: Chunk[Byte], num: Int): IO[MessageFormatError, CoapOption] =
      for {
        header       <- chunk.takeExactly(1).map(_.head)
        body         <- chunk.dropExactly(1)
        deltaTriplet <- getDeltaTriplet(header, body)
        (d, ed, od)   = deltaTriplet
        lenTriplet   <- getLengthTriplet(header, body, od)
        (l, el, ol)   = lenTriplet
        length       <- getLength(l, el)
        number       <- getNumber(d, ed, num)
        value        <- getValue(body, l, od + ol, number)
        totalOffset   = CoapOptionOffset(od.value + ol.value + length.value + 1)
      } yield body.CoapOption(d, ed, l, el, number, value, totalOffset)

    for {
      token              <- extractTokenFrom(chunk)
      remainder          <- chunk.dropExactly(header.coapTokenLength.value)
      tuple2             <- getOptionListFrom(remainder)
      (options, payload)  = tuple2
      optionsO            = Option.when(options.nonEmpty)(options)
      payloadO            = Option.when(payload.nonEmpty)(getPayloadFromWith(payload, getPayloadMediaTypeFrom(options)))
    } yield CoapBody(token, optionsO, payloadO)
  }

  type DeltaTriplet = (CoapOptionDelta, Option[CoapOptionExtendedDelta], CoapOptionOffset)

  /**
   * @return a triplet which contains a CoapOptionDelta, an Option of an CoapOptionExtendedDelta as well as
   *         the CoapOptionOffset that follows from the existence or absence of an possibly extended delta
   */
  private def getDeltaTriplet(headerByte: Byte, remainder: Chunk[Byte]): IO[MessageFormatError, DeltaTriplet] =
    (headerByte & 0xF0) >>> 4 match {
      case 13 => for {
          ext <- getFirstByteFrom(remainder) >>= (n => CoapOptionExtendedDelta(n + 13))
          del <- CoapOptionDelta(13)
        } yield (del, Some(ext), CoapOptionOffset(1))
      case 14 => for {
          ext <- mergeNextTwoBytes(remainder) >>= (n => CoapOptionExtendedDelta(n + 269))
          del <- CoapOptionDelta(14)
        } yield (del, Some(ext), CoapOptionOffset(2))
      case 15 => IO.fail(InvalidOptionDelta("15 is a reserved value."))
      case other if 0 to 12 contains other => CoapOptionDelta(other).map((_, None, CoapOptionOffset(0)))
      case error => IO.fail(InvalidOptionDelta(s"$error"))
    }

  type LengthTriplet = (CoapOptionLength, Option[CoapOptionExtendedLength], CoapOptionOffset)

  /**
   *
   * @param headerByte the option header byte
   * @param remainder everything but the option header byte
   * @param curOffset the offset that stems from a possible extended delta value -
   *                  the starting point of an extended length value
   * @return a triplet which contains a CoapOptionLength, an Option of an CoapOptionExtendedLength as well as
   *         the CoapOptionOffset that follows from the existence or absence of an possibly extended length
   */
  private def getLengthTriplet(
    headerByte : Byte,
    remainder  : Chunk[Byte],
    curOffset  : CoapOptionOffset
  ): IO[MessageFormatError, LengthTriplet] =
    headerByte & 0x0F match {
      case 13 => for {
          ext <- remainder.dropExactly(curOffset.value) >>= getFirstByteFrom >>= (n => CoapOptionExtendedLength(n + 13))
          len <- CoapOptionLength(13)
        } yield (len, Some(ext), CoapOptionOffset(2))
      case 14 => for {
          ext <- remainder.dropExactly(curOffset.value) >>= mergeNextTwoBytes >>= (n => CoapOptionExtendedLength(n + 13))
          len <- CoapOptionLength(14)
        } yield (len, Some(ext), CoapOptionOffset(2))
      case 15 => IO.fail(InvalidOptionLength("15 is a reserved length value."))
      case other if 0 to 12 contains other => CoapOptionLength(other).map((_, None, CoapOptionOffset(0)))
      case error => IO.fail(InvalidOptionLength(s"$error"))
    }

  private def getNumber(delta: CoapOptionDelta, extendedDelta: Option[CoapOptionExtendedDelta], num: Int) =
    extendedDelta match {
      case Some(ext) => CoapOptionNumber(num + ext.value)
      case None      => CoapOptionNumber(num + delta.value)
    }

  private def getLength(length: CoapOptionLength, extendedLength: Option[CoapOptionExtendedLength]) =
    extendedLength match {
      case Some(ext) => CoapOptionLength(ext.value)
      case None      => IO.succeed(length)
    }

  private def getValue(
    chunk: Chunk[Byte],
    length: CoapOptionLength,
    offset: CoapOptionOffset,
    number: CoapOptionNumber
  ): IO[MessageFormatError, CoapOptionValue] =
    chunk.dropExactly(offset.value).flatMap(_.takeExactly(length.value)).map(CoapOptionValue(number, _))

  private def getFirstByteFrom(bytes: Chunk[Byte]): IO[MessageFormatError, Int] =
    bytes.takeExactly(1).map(_.head.toInt)

  private def mergeNextTwoBytes(bytes: Chunk[Byte]): IO[MessageFormatError, Int] =
    bytes.takeExactly(2).map(chunk => ((chunk(0) << 8) & 0xFF) | (chunk(1) & 0xFF))

  private def getMsgIdFrom(third: Byte, fourth: Byte): IO[MessageFormatError, CoapId] =
    CoapId(((third & 0xFF) << 8) | (fourth & 0xFF))

  def getMsgIdFromMessage(raw: Chunk[Byte]): UIO[Option[CoapId]] =
    raw.dropExactly(2).flatMap(_.takeExactly(2)).flatMap(c => getMsgIdFrom(c(0), c(1))).option

  /**
   * The media type of the payload is defined via CoapOption #12. This option should always have
   * its content initialized as IntCoapOptionValueContent - therefore the CoapPayloadMediaType
   * can be derived via the provided int value. If this fails or the option itself is missing,
   * a placeholder MediaType is passed.
   */
  private def getPayloadMediaTypeFrom(list: Chunk[CoapOption]): CoapPayloadMediaType =
    list.find(_.coapOptionNumber.value == 12) match {
        case Some(option) => option.coapOptionValue.content match {
          case c : IntCoapOptionValueContent => CoapPayloadMediaType.fromInt(c.value)
          case _                             => SniffingMediaType
        }
        case None => SniffingMediaType
      }

  private def getPayloadFromWith(chunk: Chunk[Byte], payloadMediaType: CoapPayloadMediaType): CoapPayload =
    payloadMediaType.applyOn(chunk)

  // TODO: Implement empty message check in general and for reset messages
  private def validateMessage(header: CoapHeader, body: CoapBody): IO[MessageFormatError, CoapMessage] =
    IO.succeed(CoapMessage(header, body))

//  /**
//   * Converts a List[CoapOption] to a HashMap so that each Option is directly addressable via
//   * its CoapOptionNumber. This might be required in situations where there is a direct check
//   * for the existence of an option - probably an overkill anyway.
//   */
//  private def convertOptionListToMap(list: List[CoapOption]): HashMap[CoapOptionNumber, List[CoapOption]] =
//    list.foldRight(HashMap[CoapOptionNumber, List[CoapOption]]()) { (c, acc) =>
//      val number = c.number
//      if (acc.isDefinedAt(number))
//        if (CoapOptionNumber.getProperties(number)._4) acc + (number -> (c :: acc(number)))
//        else acc
//      else acc + (number -> List(c))
//    }
}


