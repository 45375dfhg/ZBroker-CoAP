package domain.model.config

import java.io.IOException

import zio.IO

final case class Port private(number: Int) extends AnyVal {}

sealed trait PortRangeException extends IOException
case object UnexpectedPortRangeException extends PortRangeException

object Port {
  def apply(number: Int): IO[PortRangeException, Port] =
    IO.cond(1 to 65535 contains number, new Port(number), UnexpectedPortRangeException)
}
