package utility

import domain.model.coap._
import domain.model.coap.header.fields._
import domain.model.exception._
import zio.nio.core._

object PartialTypes {

  type PartialValidMessage = PartialFunction[CoapMessage, CoapMessage]

  type IgnoredMessageWithIdOption = (GatewayError, Option[CoapId])
  type IgnoredMessageWithId       = (GatewayError, CoapId)

  type PartialGatherMessages =
    PartialFunction[(Option[SocketAddress], Either[IgnoredMessageWithId, CoapMessage]),
      (Option[SocketAddress], CoapMessage)]

  type PartialRemoveEmptyID =
    PartialFunction[(Option[SocketAddress], Either[IgnoredMessageWithIdOption, CoapMessage]),
      (Option[SocketAddress], Either[IgnoredMessageWithId, CoapMessage])]
}