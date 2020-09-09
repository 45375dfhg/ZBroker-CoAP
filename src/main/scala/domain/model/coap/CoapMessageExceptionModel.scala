package domain.model.coap

import java.io.IOException

sealed trait CoapMessageException                              extends IOException

final case class InvalidCoapVersionException (err: String)     extends CoapMessageException
final case class InvalidCoapTypeException (err: String)        extends CoapMessageException
final case class InvalidCoapTokenLengthException (err: String) extends CoapMessageException
final case class InvalidCoapCodeException (err: String)        extends CoapMessageException
final case class InvalidCoapIdException (err: String)          extends CoapMessageException
final case class InvalidOptionDelta (err: String)              extends CoapMessageException
final case class InvalidOptionLength (err: String)             extends CoapMessageException
final case class InvalidOptionValue (err: String)              extends CoapMessageException

case object InvalidPayloadMarker                               extends CoapMessageException
case object InvalidCoapChunkSize                               extends CoapMessageException
case object InvalidCoapOptionNumber                            extends CoapMessageException
case object InvalidCoapOptionLength                            extends CoapMessageException