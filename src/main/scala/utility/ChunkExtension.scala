package utility

import domain.model.exception._
import zio.{Chunk, IO, NonEmptyChunk}

object ChunkExtension {

  implicit class ChunkExtension[A](chunk: Chunk[A]) {
    def leftPadTo(len: Int, element: A): Chunk[A] =
      Chunk.fill(len - chunk.size)(element) ++ chunk

    def takeExactly(n: Int): IO[MessageFormatError, Chunk[A]] = {
      val elements = chunk.take(n)
      if (elements.lengthCompare(n) >= 0) IO.succeed(elements)
      else IO.fail(InvalidCoapChunkSize(s"Failed to take $n elements, only ${chunk.size} available."))
    }

    def takeExactlyN(n: Int): IO[GatewayError, NonEmptyChunk[A]] = {
      val elements = chunk.take(n)

      if (n > 0 && elements.lengthCompare(n) >= 0)
        NonEmptyChunk.fromChunk(elements) match {
          case Some(nonEmpty) => IO.succeed(nonEmpty)
          case None           => IO.fail(UnreachableCodeError)
        }
      else IO.fail(InvalidCoapChunkSize(s"Failed to take $n elements, only ${chunk.size} available."))
    }

    def dropExactly(n: Int): IO[MessageFormatError, Chunk[A]] =
      if (chunk.lengthCompare(n) >= 0) IO.succeed(chunk.drop(n))
      else IO.fail(InvalidCoapChunkSize(s"Failed to drop $n elements, only ${chunk.size} available."))

    def tailOption: Option[Chunk[A]] =
      if (chunk.drop(1).isEmpty) None else Some(chunk.tail)

  }
}