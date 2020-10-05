package domain.model.coap.body

import domain.model.coap.header.fields.CoapTokenLength
import domain.model.exception._
import utility.ChunkExtension.ChunkExtension
import zio._

final case class CoapToken(value: NonEmptyChunk[Byte]) extends AnyVal

case object CoapToken {

  def fromBodyWith(chunk: Chunk[Byte], coapTokenLength: CoapTokenLength): IO[GatewayError, Option[CoapToken]] =
    if (coapTokenLength.value > 0) chunk.takeExactlyN(coapTokenLength.value).map(t => Some(CoapToken(t)))
    else ZIO.none
}