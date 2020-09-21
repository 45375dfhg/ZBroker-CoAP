package domain.model.exception

abstract class SuccessfulFailure extends GatewayError

case object NoResponseAvailable extends SuccessfulFailure {
  type NoResponseAvailable = NoResponseAvailable.type

  override def msg = "This message does not require a response"
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