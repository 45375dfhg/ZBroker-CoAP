package utility

import domain.model.coap._
import zio.Chunk

object ChunkExtension {

  implicit class ChunkExtension[A](chunk: Chunk[A]) {
    def leftPadTo(len: Int, element: A): Chunk[A] =
      Chunk.fill(len - chunk.size)(element) ++ chunk

    def takeExactly(n: Int): Either[MessageFormatError, Chunk[A]] = {
      val elements = chunk.take(n)
      if (elements.lengthCompare(n) >= 0) Right(elements)
      else Left(InvalidCoapChunkSize(s"Failed to take $n elements, only ${chunk.size} available."))
    }

    def dropExactly(n: Int): Either[MessageFormatError, Chunk[A]] =
      if (chunk.lengthCompare(n) >= 0) Right(chunk.drop(n))
      else Left(InvalidCoapChunkSize(s"Failed to drop $n elements, only ${chunk.size} available."))

  }
}