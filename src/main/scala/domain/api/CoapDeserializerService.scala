package domain.api

import domain.model.coap.CoapMessage
import domain.model.coap.header.fields.CoapId
import domain.model.exception.GatewayError
import zio._


object CoapDeserializerService {

  // TODO: A BIT EMPTY HERE
  def parseCoapMessage(chunk: Chunk[Byte]): UIO[Either[(GatewayError, Option[CoapId]), CoapMessage]] =
    CoapMessage.fromDatagram(chunk).flatMapError(err => UIO.succeed(err) <*> CoapId.recoverFrom(chunk)).either

  def parseCoapMessageWithoutErr(chunk: Chunk[Byte]): UIO[Either[UIO[Option[CoapId]], CoapMessage]] =
    CoapMessage.fromDatagram(chunk).orElseFail(CoapId.recoverFrom(chunk)).either
}

