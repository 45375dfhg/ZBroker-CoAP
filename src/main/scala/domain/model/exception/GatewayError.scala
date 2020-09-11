package domain.model.exception

trait GatewayError extends Exception {
  def msg: String
  def fullMsg: String =
    this.getClass.getSimpleName + ": " + msg + " in: " + this.getStackTrace.mkString("Array(", ", ", ")")
}

final case class SystemError(msg: String, cause: Throwable) extends GatewayError {
  override def fullMsg: String = super.fullMsg + cause
}