package domain

final case class Gateway (
    in:  Unidirectional, 
    out: Unidirectional
)

final case class Unidirectional (
    in:  Endpoint,
    out: Endpoint
)

sealed trait Endpoint
final case class UDPEndpoint(protocol: Protocol) extends Endpoint
final case class WebsocketEndpoint(protocol: Protocol) extends Endpoint

sealed trait Protocol
final case object CoAP extends Protocol
final case object JSON extends Protocol

