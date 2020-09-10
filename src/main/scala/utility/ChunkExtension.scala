package utility

import domain.model.coap._
import zio.Chunk

object ChunkExtension {

  implicit class ChunkExtension[A](chunk: Chunk[A]) {
    def leftPadTo(len: Int, element: A): Chunk[A] =
      Chunk.fill(len - chunk.size)(element) ++ chunk

    def takeExactly(n: Int): Either[CoapMessageException, Chunk[A]] = {
      val elements = chunk.take(n)
      if (elements.lengthCompare(n) >= 0) Right(elements)
      else Left(InvalidCoapChunkSize)
    }

    def dropExactly(n: Int): Either[CoapMessageException, Chunk[A]] =
      if (chunk.lengthCompare(n) >= 0) Right(chunk.drop(n))
      else Left(InvalidCoapChunkSize)

  }
}