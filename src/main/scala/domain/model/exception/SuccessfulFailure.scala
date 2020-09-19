package domain.model.exception

abstract class SuccessfulFailure extends GatewayError

case object NoResponseAvailable extends SuccessfulFailure {
  override def msg = "This message does not require a response"
}

case object MissingCoapId extends SuccessfulFailure {
  override def msg = "The message is not valid and contains no ID."
}

case object MissingAddress extends SuccessfulFailure {
  override def msg = ""
}