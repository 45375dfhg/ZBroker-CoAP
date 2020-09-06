package utility

import domain.model.coap._
import zio.Chunk

object ChunkExtension {

  implicit class ChunkFailOnMissingElements[A](chunk: Chunk[A]) {
    def takeExactly(n: Int): Either[CoapMessageException, Chunk[A]] = {
      val elements = chunk.take(n)
      if (elements.lengthCompare(n) >= 0) Right(elements)
      else Left(InvalidCoapChunkSize)
    }
    def dropExactly(n: Int): Either[CoapMessageException, Chunk[A]] = {
      if (chunk.lengthCompare(n) >= 0) Right(chunk.drop(n))
      else Left(InvalidCoapChunkSize)
    }
  }
}