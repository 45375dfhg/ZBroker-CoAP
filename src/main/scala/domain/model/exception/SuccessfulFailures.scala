package domain.model.exception

abstract class SuccessfulFailures extends GatewayError

case object NoResponse extends SuccessfulFailures {
  override def msg = "This message does not require a response"
}