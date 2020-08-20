package domain.model.protocol

import domain.model._
import domain.model.coap._

sealed trait Protocol
final case class COAP(pipeline: ByteBufferWrapper => ChunkByteWrapper => COAPMessage) extends Protocol
// final case class JSON(pipeline: JSONWrapper) extends Protocol