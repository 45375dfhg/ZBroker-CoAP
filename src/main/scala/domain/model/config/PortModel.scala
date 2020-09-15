package domain.model

import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops.toCoercibleIdOps

import domain.model.exception.GatewayError

import zio.IO

package object PortModel {
  @newtype class Port private(val value: Int)

  sealed trait PortRangeException extends GatewayError
  final case class UnexpectedPortRangeException(err: String) extends PortRangeException {
    override def msg: String = err
  }

  object Port {
    def apply(value: Int): IO[PortRangeException, Port] =
      IO.cond(1 to 65535 contains value, value.coerce, UnexpectedPortRangeException(s"$value"))
  }
}

