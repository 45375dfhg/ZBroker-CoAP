package domain.api

import domain.model.coap._
import domain.model.coap.body.CoapToken
import domain.model.coap.header.fields._
import utility.PartialTypes._
import zio.Chunk

object ResponseService {

  def constructAckFrom(msg: CoapMessage): Chunk[Byte] =
    CoapMessage.asAckFrom(msg).toByteChunk

  def constructResetFrom(id: CoapId): Chunk[Byte] =
    CoapMessage.asResetWith(id).toByteChunk

  def constructResetWith(id: CoapId, token: CoapToken): Chunk[Byte] =
    ???
}
