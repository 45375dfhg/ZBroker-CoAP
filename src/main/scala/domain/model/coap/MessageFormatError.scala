package domain.model.coap

import domain.model.exception.GatewayError


sealed trait MessageFormatError                       extends GatewayError


final case class InvalidCoapVersion(msg: String)      extends MessageFormatError

final case class InvalidCoapType(msg: String)         extends MessageFormatError

final case class InvalidCoapTokenLength(msg: String)  extends MessageFormatError

final case class InvalidCoapCode(msg: String)         extends MessageFormatError

final case class InvalidCoapId(msg: String)           extends MessageFormatError

final case class InvalidOptionDelta(msg: String)      extends MessageFormatError

final case class InvalidOptionLength(msg: String)     extends MessageFormatError

final case class InvalidOptionValue(msg: String)      extends MessageFormatError

final case class InvalidCoapOptionNumber(msg: String) extends MessageFormatError

case object InvalidPayloadMarker                      extends MessageFormatError {
  override def msg = "Promised Payload missing."
}

final case class InvalidCoapChunkSize(msg: String)    extends MessageFormatError

final case class InvalidCoapOptionLength(msg: String) extends MessageFormatError

final case class InvalidPayloadStructure(msg: String) extends MessageFormatError

case object InvalidEmptyMessage extends MessageFormatError {
  override def msg = "Empty message was promised, yet contains content in body."
}
