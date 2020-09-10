package domain.model.coap

import java.io.IOException

sealed trait CoapMessageException                             extends IOException {
  def msg: String
  def fullMsg: String = this.getClass.getSimpleName + ": " + msg
}

final case class InvalidCoapVersionException(err: String)     extends CoapMessageException {
  override def msg: String = err
}
final case class InvalidCoapTypeException(err: String)        extends CoapMessageException {
  override def msg: String = err
}
final case class InvalidCoapTokenLengthException(err: String) extends CoapMessageException {
  override def msg: String = err
}
final case class InvalidCoapCodeException(err: String)        extends CoapMessageException {
  override def msg: String = err
}
final case class InvalidCoapIdException(err: String)          extends CoapMessageException {
  override def msg: String = err
}
final case class InvalidOptionDelta(err: String)              extends CoapMessageException {
  override def msg: String = err
}
final case class InvalidOptionLength(err: String)             extends CoapMessageException {
  override def msg: String = err
}
final case class InvalidOptionValue(err: String)              extends CoapMessageException {
  override def msg: String = err
}
final case class InvalidCoapOptionNumber(err: String)         extends CoapMessageException {
  override def msg: String = err
}


// TODO: Implement the msg functions below
case object InvalidPayloadMarker                               extends CoapMessageException {
  override def msg: String = ""
}
case object InvalidCoapChunkSize                               extends CoapMessageException {
  override def msg: String = ""
}

case object InvalidCoapOptionLength                            extends CoapMessageException {
  override def msg: String = ""
}
