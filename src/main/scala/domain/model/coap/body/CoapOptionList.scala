package domain.model.coap.body

import domain.model.coap.body.fields._
import domain.model.coap.header.fields._
import domain.model.exception.GatewayError
import utility.ChunkExtension._
import zio._

final case class CoapOptionList(value: NonEmptyChunk[CoapOption]) extends AnyVal

case object CoapOptionList {

  def fromBody(datagram: Chunk[Byte], coapTokenLength: CoapTokenLength) = {
    for {
      body <- datagram.dropExactly(coapTokenLength.value)

    } yield ()
  }

  private def getOptions(chunk: Chunk[Byte], acc: Chunk[CoapOption] = Chunk.empty, num: Int = 0) =
    chunk.headOption match {
      case Some(byte) => if (byte == marker) IO.succeed(acc)
      else
      case None       => IO.succeed(acc)
    }

  private def getNextOption(chunk: Chunk[Byte], num: Int) =
    for {
      header       <- chunk.takeExactly(1).map(_.head)
      body         <- chunk.dropExactly(1)
      optionDelta  <- getCoapOptionDelta(header, body)
      optionLength <- getCoapOptionLength(header, body, optionDelta.offset)

    } yield ()

  private def getCoapOptionDelta(header: Byte, body: Chunk[Byte]): ZIO[Any, GatewayError, CoapOptionDelta] =
    CoapOptionDelta.fromOptionHeader(header).flatMap(CoapOptionDelta.extend(_, body))

  private def getCoapOptionLength(header: Byte, body: Chunk[Byte], offset: Int) = ???


  private val marker: Byte = 0xFF.toByte

  // def fromBody(chunk: Chunk[Byte], acc: Chunk[CoapOption] = Chunk.empty, num: Int = 0) = ???
}