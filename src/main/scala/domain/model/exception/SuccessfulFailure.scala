package domain.model.exception

abstract class SuccessfulFailure extends GatewayError

case object NoResponseAvailable extends SuccessfulFailure {
  type NoResponseAvailable = NoResponseAvailable.type

  override def msg = "This message does not require a response"
}

case object InvalidCoapMessage extends SuccessfulFailure {
  override def msg = "The parsing of the datagram was unsuccessful."
}

case object MissingCoapId extends SuccessfulFailure {
  override def msg = "The message is not valid and contains no ID."
}

case object MissingAddress extends SuccessfulFailure {
  override def msg = "No socket address was provided."
}

case object MissingOptions extends SuccessfulFailure {
  override def msg = "The message does not contain any options."
}

case object MissingRoutes extends SuccessfulFailure {
  override def msg = "The message does not contain any options with an Uri-Path."
}

case object MissingPayload extends SuccessfulFailure {
  override def msg = "No payload was provided which can be passed to subscribers."
}

case object UnsupportedPayload extends SuccessfulFailure {
  override def msg = "Payload format is not supported."
}

case object UnsharablePayload extends SuccessfulFailure {
  type UnsharablePayload = UnsharablePayload.type
  override def msg = "The message is not meant to be shared."
}

case object Duplicate extends SuccessfulFailure {
  type Duplicate = Duplicate.type
  override def msg = "This message is a duplicate."
}