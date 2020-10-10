package domain.model.coap.header

import domain.model.coap.header.fields._
import domain.model.exception._
import utility.classExtension.ChunkExtension.ChunkExtension
import zio._

final case class CoapHeader(
  coapVersion     : CoapVersion,
  coapType        : CoapType,
  coapTokenLength : CoapTokenLength,
  coapCodePrefix  : CoapCodePrefix,
  coapCodeSuffix  : CoapCodeSuffix,
  coapId          : CoapId
) {
  def toByteChunk: Chunk[Byte] =
    ((coapVersion.toHeaderPart + coapType.toHeaderPart + coapTokenLength.toHeaderPart) +:
      ((coapCodePrefix.toHeaderPart + coapCodeSuffix.toHeaderPart) +:
        coapId.toHeaderPart)).map(_.toByte)
}

object CoapHeader {

  def fromDatagram(datagram: Chunk[Byte]): IO[MessageFormatError, CoapHeader] = {
    for {
      b <- datagram.takeExactly(4)
      (b1, b2, b3, b4) = (b(0), b(1), b(2), b(3))
      v <- CoapVersion.fromByte(b1)
      t <- CoapType.fromByte(b1)
      l <- CoapTokenLength.fromByte(b1)
      p <- CoapCodePrefix.fromByte(b2)
      s <- CoapCodeSuffix.fromByte(b2)
      i <- CoapId.fromBytes(b3, b4)
    } yield CoapHeader(v, t, l ,p, s, i)
  }

  def ack(id: CoapId) = CoapHeader(
    CoapVersion.default,
    CoapType.acknowledge,
    CoapTokenLength.empty,
    CoapCodePrefix.empty,
    CoapCodeSuffix.empty,
    id
  )

  def reset(id: CoapId) = CoapHeader(
    CoapVersion.default,
    CoapType.reset,
    CoapTokenLength.empty,
    CoapCodePrefix.empty,
    CoapCodeSuffix.empty,
    id
  )
}

