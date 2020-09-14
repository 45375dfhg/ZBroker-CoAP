package domain.model.coap

import domain.model.exception.GatewayError

sealed trait MessageFormatError                       extends GatewayError

final case class InvalidCoapVersion(err: String)      extends MessageFormatError {
  override def msg: String = err
}
final case class InvalidCoapType(err: String)         extends MessageFormatError {
  override def msg: String = err
}
final case class InvalidCoapTokenLength(err: String)  extends MessageFormatError {
  override def msg: String = err
}
final case class InvalidCoapCode(err: String)         extends MessageFormatError {
  override def msg: String = err
}
final case class InvalidCoapId(err: String)           extends MessageFormatError {
  override def msg: String = err
}
final case class InvalidOptionDelta(err: String)      extends MessageFormatError {
  override def msg: String = err
}
final case class InvalidOptionLength(err: String)     extends MessageFormatError {
  override def msg: String = err
}
final case class InvalidOptionValue(err: String)      extends MessageFormatError {
  override def msg: String = err
}
final case class InvalidCoapOptionNumber(err: String) extends MessageFormatError {
  override def msg: String = err
}
final case class InvalidPayloadMarker(err: String)    extends MessageFormatError {
  override def msg: String = err
}
final case class InvalidCoapChunkSize(err: String)    extends MessageFormatError {
  override def msg: String = err
}
final case class InvalidCoapOptionLength(err: String) extends MessageFormatError {
  override def msg: String = err
}
final case class InvalidPayloadStructure(err: String) extends MessageFormatError {
  override def msg: String = err
}
case object InvalidEmptyMessage extends MessageFormatError {
  override def msg = "Empty message was promised, yet contains content in body."
}
