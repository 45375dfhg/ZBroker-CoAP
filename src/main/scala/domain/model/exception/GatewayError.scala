package domain.model.exception

// shout out to @Rudder
trait GatewayError extends Exception with Serializable {

  def cause(value: Any): String =
    this.getClass.getSimpleName + "failed because of the following value: " + value.toString

  def msg: String

  def fullMsg: String =
    this.getClass.getSimpleName + ": " + msg + " in: " + this.getStackTrace.mkString("Array(", ", ", ")")
}

final case class UnexpectedError(msg: String) extends GatewayError

final case class SystemError(msg: String, cause: Throwable) extends GatewayError {
  override def fullMsg: String = super.fullMsg + cause
}

