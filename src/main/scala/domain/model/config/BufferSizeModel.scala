package domain.model.config

import java.io.IOException

import zio.IO

// TODO: REWRITE TO NEWTYPE
final case class BufferSize private(val value: Int) extends AnyVal {}

sealed trait BufferSizeException extends IOException
case object UnexpectedBufferSizeException extends BufferSizeException

object BufferSize {
  def apply(value: Int): IO[BufferSizeException, BufferSize] =
    IO.cond(1 to 65535 contains value, new BufferSize(value), UnexpectedBufferSizeException)
}