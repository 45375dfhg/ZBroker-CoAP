package domain.model.coap

import zio.Chunk

final case class CoapBody(
  token   : Option[CoapToken],
  options : Option[Chunk[CoapOption]],
  payload : Option[CoapPayload]
)

object CoapBody {
  val empty = CoapBody(None, None, None)
}

// TODO: Implement
final case class CoapToken(value: Chunk[Byte])

