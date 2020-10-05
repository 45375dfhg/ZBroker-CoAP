package domain.model.exception

abstract class UnexpectedError extends GatewayError

case object MissingBrokerBucket extends UnexpectedError {

  type MissingBrokerBucket = MissingBrokerBucket.type

  def msg: String = "WARNING: A bucket returned no value. The broker is inconsistent."
}

case object MissingSubscriber extends UnexpectedError {

  type MissingSubscriber = MissingSubscriber.type

  def msg: String = "WARNING: The subscriber was deleted from an unintended locality. FAULTY MODEL."
}

case object UnreachableCodeError extends UnexpectedError {

  type UnreachableCodeError = UnreachableCodeError.type

  def msg: String = "WARNING. A unreachable part of the code has been reached. FAULTY MODEL."
}