package domain.model.coap.body

import domain.model.coap.body.fields._
import domain.model.coap.header.fields._
import domain.model.exception.GatewayError
import utility.ChunkExtension._
import zio._

final case class CoapOptionList(value: NonEmptyChunk[CoapOption]) extends AnyVal

case object CoapOptionList {

  def fromBody(datagram: Chunk[Byte], coapTokenLength: CoapTokenLength): IO[GatewayError, Option[CoapOptionList]] =
    datagram.dropExactly(coapTokenLength.value).flatMap(body => getOptions(body))

  private def getOptions(
    chunk: Chunk[Byte],
    acc: Chunk[CoapOption] = Chunk.empty,
    num: Int = 0
  ): IO[GatewayError, Option[CoapOptionList]] =
    chunk.headOption match {
      case None       => IO.succeed(NonEmptyChunk.fromChunk(acc).map(CoapOptionList(_)))
      case Some(byte) =>
        if (byte == marker) IO.succeed(NonEmptyChunk.fromChunk(acc).map(CoapOptionList(_)))
        else getNextOption(chunk, num) >>= { case (o, n) => getOptions(chunk.drop(n), acc :+ o, o.coapOptionNumber.value) }
    }

  private def getNextOption(chunk: Chunk[Byte], num: Int): IO[GatewayError, (CoapOption, Int)] =
    for {
      header       <- chunk.takeExactly(1).map(_.head)
      body         <- chunk.dropExactly(1)
      optionDelta  <- CoapOptionDelta.fromDatagram(header, body)
      optionLength <- CoapOptionLength.fromDatagram(header, body, optionDelta.offset)
      optionNumber <- CoapOptionNumber(optionDelta.value + num)
      optionValue  <- CoapOptionValue.make(body, optionLength, optionDelta.offset + optionLength.offset, optionNumber)
      n             = optionDelta.offset + optionLength.offset + optionLength.value + 1
    } yield (CoapOption(optionDelta, optionLength, optionNumber, optionValue), n)

  private val marker: Byte = 0xFF.toByte
}