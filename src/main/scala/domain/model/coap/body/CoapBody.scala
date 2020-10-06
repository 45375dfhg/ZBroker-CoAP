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

  def fromDatagramWith(datagram: Chunk[Byte], header: CoapHeader): IO[GatewayError, CoapBody] =
    for {
      body     <- datagram.dropExactly(4)
      token    <- CoapToken.fromBodyWith(body, header.coapTokenLength)
      options  <- CoapOptionList.fromBodyExcluding(datagram, header.coapTokenLength)
      offset    = header.coapTokenLength.value + options.fold(0)(_.offset)
      payload  <- CoapPayload.fromWithExcluding(body, CoapPayloadMediaType.fromOption(options), offset)
    } yield CoapBody(token, options, payload)

    val empty = CoapBody(None, None, None)
}

