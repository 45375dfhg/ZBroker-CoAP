package domain.model.coap.body

import domain.model.coap.body.fields._
import domain.model.coap.header.fields._
import domain.model.exception.GatewayError
import utility.classExtension.ChunkExtension._

import zio._


final case class CoapOptionList(value: NonEmptyChunk[CoapOption]) extends AnyVal {
  def offset = value.foldLeft(0) { (acc, c) =>
    acc + c.coapOptionDelta.offset + c.coapOptionLength.offset + c.coapOptionLength.value + 1
  }

  // TODO: Wrong place?
  def payloadMediaType: CoapPayloadMediaType =
    value.find(_.coapOptionNumber.value == 12) match {
      case Some(element) => element.coapOptionValue.content match {
        case c : IntCoapOptionValueContent => CoapPayloadMediaType.fromInt(c.value)
        case _                             => SniffingMediaType // TODO: Technically this is unexpected parsing error!
      }
      case None => SniffingMediaType
    }

}

case object CoapOptionList {

  def fromBodyExcluding(body: Chunk[Byte], coapTokenLength: CoapTokenLength): IO[GatewayError, Option[CoapOptionList]] =
    body.dropExactly(coapTokenLength.value).flatMap(body => getOptions(body))

  private def getOptions(
    chunk: Chunk[Byte],
    acc: Chunk[CoapOption] = Chunk.empty,
    num: Int = 0
  ): IO[GatewayError, Option[CoapOptionList]] =
    chunk.headOption match {
      case None       => IO.succeed(NonEmptyChunk.fromChunk(acc).map(CoapOptionList(_)))
      case Some(byte) =>
        if (byte == marker) IO.succeed(NonEmptyChunk.fromChunk(acc).map(CoapOptionList(_)))
        else getNextOption(chunk, num) >>= { case (e, o) => getOptions(chunk.drop(o), acc :+ e, e.coapOptionNumber.value) }
    }

  private def getNextOption(chunk: Chunk[Byte], num: Int): IO[GatewayError, (CoapOption, Int)] =
    for {
      header       <- chunk.takeExactly(1).map(_.head)
      body         <- chunk.dropExactly(1)
      optionDelta  <- CoapOptionDelta.fromWith(header, body)
      optionLength <- CoapOptionLength.fromWithExcluding(header, body, optionDelta.offset)
      optionNumber <- CoapOptionNumber.from(optionDelta.value + num)
      optionValue  <- CoapOptionValue.fromWithExcluding(body, optionLength, optionNumber, optionDelta.offset + optionLength.offset)
      n             = 1 + optionDelta.offset + optionLength.offset + optionLength.value

    } yield (CoapOption(optionDelta, optionLength, optionNumber, optionValue), n)

  private val marker: Byte = 0xFF.toByte
}