package domain.model

import domain.model.exception.GatewayError
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops.toCoercibleIdOps
import zio.IO

package object FiberModel {

  sealed trait FiberNumberException extends GatewayError
  final case class UnexpectedFiberNumberException(err: String) extends FiberNumberException {
    override def msg: String = err
  }

  @newtype class FiberNumber private(val value: Int)

  object FiberNumber {
    def apply(value: Int): IO[FiberNumberException, FiberNumber] =
      IO.cond(1 to Int.MaxValue contains value, value.coerce, UnexpectedFiberNumberException(s"$value"))
  }
}