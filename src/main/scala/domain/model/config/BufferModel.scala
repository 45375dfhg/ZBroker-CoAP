package domain.model

import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops.toCoercibleIdOps

import domain.model.exception.GatewayError

import zio.IO

package object BufferModel {
  sealed trait BufferSizeException extends GatewayError
  final case class UnexpectedBufferSizeException(err: String) extends BufferSizeException {
    override def msg: String = err
  }

  @newtype class BufferSize private(val value: Int)

  object BufferSize {
    def apply(value: Int): IO[BufferSizeException, BufferSize] =
      IO.cond(1 to 65535 contains value, value.coerce, UnexpectedBufferSizeException(s"$value"))
  }
}





