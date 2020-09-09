package domain.model.coap

import zio.Chunk

/*
 * The entry point is in {{{CoapMessage}}}
 */

final case class CoapBody(
  token   : Option[CoapToken],
  options : Option[List[CoapOption]],
  payload : Option[CoapPayload]
)

final case class CoapToken(value: Chunk[Byte])

