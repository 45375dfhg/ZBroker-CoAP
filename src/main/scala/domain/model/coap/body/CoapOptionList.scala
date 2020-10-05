package domain.model.coap.body

import domain.model.coap.header.fields._
import utility.ChunkExtension._
import zio._

final case class CoapOptionList(value: NonEmptyChunk[CoapOption]) extends AnyVal

case object CoapOptionList {

  def fromBody(datagram: Chunk[Byte], coapTokenLength: CoapTokenLength) = {
    for {
      body <- datagram.dropExactly(coapTokenLength.value)

    } yield ()
  }

  private def getOptions(remainder: Chunk[Byte], acc: Chunk[CoapOption] = Chunk.empty, num: Int = 0) =


  // def fromBody(chunk: Chunk[Byte], acc: Chunk[CoapOption] = Chunk.empty, num: Int = 0) = ???
}