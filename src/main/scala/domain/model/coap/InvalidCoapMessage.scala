package domain.model.coap

import domain.model.exception.GatewayError



sealed trait InvalidCoapMessage                               extends GatewayError

final case class InvalidCoapVersionException(err: String)     extends InvalidCoapMessage {
  override def msg: String = err
}
final case class InvalidCoapTypeException(err: String)        extends InvalidCoapMessage {
  override def msg: String = err
}
final case class InvalidCoapTokenLengthException(err: String) extends InvalidCoapMessage {
  override def msg: String = err
}
final case class InvalidCoapCodeException(err: String)        extends InvalidCoapMessage {
  override def msg: String = err
}
final case class InvalidCoapIdException(err: String)          extends InvalidCoapMessage {
  override def msg: String = err
}
final case class InvalidOptionDelta(err: String)              extends InvalidCoapMessage {
  override def msg: String = err
}
final case class InvalidOptionLength(err: String)             extends InvalidCoapMessage {
  override def msg: String = err
}
final case class InvalidOptionValue(err: String)              extends InvalidCoapMessage {
  override def msg: String = err
}
final case class InvalidCoapOptionNumber(err: String)         extends InvalidCoapMessage {
  override def msg: String = err
}
final case class InvalidPayloadMarker(err: String)            extends InvalidCoapMessage {
  override def msg: String = err
}
final case class InvalidCoapChunkSize(err: String)            extends InvalidCoapMessage {
  override def msg: String = err
}
final case class InvalidCoapOptionLength(err: String)         extends InvalidCoapMessage {
  override def msg: String = err
}
