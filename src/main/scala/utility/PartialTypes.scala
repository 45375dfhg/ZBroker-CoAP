package utility

import domain.api.CoapDeserializerService._
import domain.model.coap._
import zio.nio.core._

object PartialTypes {

  type PartialValidMessage = PartialFunction[CoapMessage, CoapMessage]

  type PartialGatherMessages =
    PartialFunction[(Option[SocketAddress], Either[IgnoredMessageWithId, CoapMessage]),
      (Option[SocketAddress], CoapMessage)]

  type PartialRemoveEmptyID =
    PartialFunction[(Option[SocketAddress], Either[IgnoredMessageWithIdOption, CoapMessage]),
      (Option[SocketAddress], Either[IgnoredMessageWithId, CoapMessage])]
}