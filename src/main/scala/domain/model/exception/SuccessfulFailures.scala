package domain.model.exception

abstract class SuccessfulFailures extends GatewayError

case object NoResponseAvailable extends SuccessfulFailures {
  type NoResponseAvailable = NoResponseAvailable.type

  override def msg = "This message does not require a response"
}

case object MissingCoapId extends SuccessfulFailures {
  override def msg = "The message is not valid and contains no ID."
}

case object MissingAddress extends SuccessfulFailures {
  override def msg = ""
}