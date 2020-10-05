package domain.model.coap.body

import domain.model.coap.header.CoapHeader
import domain.model.exception._
import utility.ChunkExtension._
import zio._

final case class CoapBody(
  token   : Option[CoapToken],
  options : Option[CoapOptionList],
  payload : Option[CoapPayload]
)

object CoapBody {

  def fromDatagramWith(datagram: Chunk[Byte], header: CoapHeader) = {

    for {
      token   <- CoapToken.fromBody(datagram, header.coapTokenLength)
      options <- CoapOptionList.fromBody(datagram, header.coapTokenLength)
    } yield ()


    type OptionListAndPayload = (Chunk[CoapOption], Chunk[Byte])

    type OptionTuple = (Option[NonEmptyChunk[CoapOption]], Option[CoapPayload])



    def getOptionListFrom2(
      chunk: Chunk[Byte], acc: Chunk[CoapOption] = Chunk.empty, num: Int = 0
    ): IO[MessageFormatError, OptionTuple] =
      chunk.headOption match {
        case Some(b) =>
          if (b == 0xFF.toByte)
            IO.fromOption(NonEmptyChunk.fromChunk(chunk.drop(1)))
              .orElseFail(InvalidPayloadMarker)
              .map(load => (NonEmptyChunk.fromChunk(acc), Some(CoapPayload.fromWith(load, )))

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
      } yield CoapOption(d, ed, l, el, number, value, totalOffset)

    for {
      token              <- extractTokenFrom(chunk)
      remainder          <- chunk.dropExactly(header.coapTokenLength.value)
      tuple2             <- getOptionListFrom(remainder)
      (options, payload)  = tuple2
      optionsO            = Option.when(options.nonEmpty)(options)
      payloadO            = Option.when(payload.nonEmpty)(getPayloadFromWith(payload, getPayloadMediaTypeFrom(options)))
    } yield CoapBody(token, optionsO, payloadO)
  }

  val empty = CoapBody(None, None, None)
}






//final case class CoapToken(value: NonEmptyChunk[Byte]) extends AnyVal
//
//case object CoapToken {
//
//  def fromBody(chunk: Chunk[Byte], length: Int): IO[GatewayError, Option[CoapToken]] =
//    if (length > 0) chunk.takeExactlyN(length).map(t => Some(CoapToken(t)))
//    else ZIO.none
//}

