package utility

import domain.model.coap._
import zio.{Chunk, IO}

object ChunkExtension {

  implicit class ChunkExtension[A](chunk: Chunk[A]) {
    def leftPadTo(len: Int, element: A): Chunk[A] =
      Chunk.fill(len - chunk.size)(element) ++ chunk

    def takeExactly(n: Int): IO[MessageFormatError, Chunk[A]] = {
      val elements = chunk.take(n)
      if (elements.lengthCompare(n) >= 0) IO.succeed(elements)
      else IO.fail(InvalidCoapChunkSize(s"Failed to take $n elements, only ${chunk.size} available."))
    }

    def dropExactly(n: Int): IO[MessageFormatError, Chunk[A]] =
      if (chunk.lengthCompare(n) >= 0) IO.succeed(chunk.drop(n))
      else IO.fail(InvalidCoapChunkSize(s"Failed to drop $n elements, only ${chunk.size} available."))

  }
}