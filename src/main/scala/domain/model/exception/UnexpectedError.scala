package domain.model.exception

abstract class UnexpectedError extends GatewayError

case object MissingBrokerBucket extends UnexpectedError {

  type MissingBrokerBucket = MissingBrokerBucket.type

  def msg: String = "WARNING: A bucket returned no value. The broker is inconsistent."
}