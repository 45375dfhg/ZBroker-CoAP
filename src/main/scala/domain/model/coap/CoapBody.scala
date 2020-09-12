package domain.model.coap

import zio.Chunk

final case class CoapBody(
  token   : Option[CoapToken],
  options : Option[List[CoapOption]],
  payload : Option[CoapPayload]
)

// TODO: Implement
final case class CoapToken(value: Chunk[Byte])

