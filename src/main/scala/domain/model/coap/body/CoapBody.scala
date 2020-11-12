package domain.model.coap.body

import domain.model.coap.header.CoapHeader
import domain.model.exception._
import utility.classExtension.ChunkExtension._
import zio._

final case class CoapBody(
  token   : Option[CoapToken],
  options : Option[CoapOptionList],
  payload : Option[CoapPayload]
) {
  def toByteChunk: Chunk[Byte] =
    tokenToByteChunk ++ optionsToByteChunk ++ payloadToByteChunk

  def tokenToByteChunk: Chunk[Byte] =
    token.fold(Chunk[Byte]())(_.toByteChunk)

  def optionsToByteChunk: Chunk[Byte] =
    options.fold(Chunk[Byte]())(_.value.toChunk.flatMap(_.toByteChunk))

  def payloadToByteChunk: Chunk[Byte] =
    payload.fold(Chunk[Byte]())(_.toByteChunk)
}

object CoapBody {

  def fromDatagramWith(datagram: Chunk[Byte], header: CoapHeader): IO[GatewayError, CoapBody] =
    for {
      body     <- datagram.dropExactly(4)
      token    <- CoapToken.fromBodyWith(body, header.coapTokenLength)
      options  <- CoapOptionList.fromBodyExcluding(body, header.coapTokenLength)
      offset    = header.coapTokenLength.value + options.fold(0)(_.offset)
      payload  <- CoapPayload.fromWithExcluding(body, CoapPayloadMediaType.fromOption(options), offset)
    } yield CoapBody(token, options, payload)

  /**
   * Creates a CoapBody that copies a requests optional token but besides that is empty.
   * This is used to construct responses (without piggy-backing)
   */
  def asEmptyResponse(tokenO: Option[CoapToken]) =
    CoapBody(tokenO, None, None)

  val empty = CoapBody(None, None, None)
}

