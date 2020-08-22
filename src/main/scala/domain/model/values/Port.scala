package domain.model.values

import java.io.IOException

class Port private(val number: Int) extends AnyVal {}

sealed trait PortRangeException extends IOException
case object UnexpectedPortRangeException extends PortRangeException

object Port {

  def apply(number: Int): Either[PortRangeException, Port] =
    Either.cond(1 to 65535 contains number, new Port(number), UnexpectedPortRangeException)

}
