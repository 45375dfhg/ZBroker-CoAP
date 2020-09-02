package domain.model.values

import java.io.IOException

final case class BufferSize private(val value: Int) extends AnyVal {}

sealed trait BufferSizeException extends IOException
case object UnexpectedBufferSizeException extends BufferSizeException

object BufferSize {
  def apply(value: Int): Either[BufferSizeException, BufferSize] =
    Either.cond(1 to 65535 contains value, new BufferSize(value), UnexpectedBufferSizeException)
}