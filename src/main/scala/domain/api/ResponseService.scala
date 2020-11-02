package domain.api


import domain.model.coap._
import domain.model.coap.header.fields._
import utility.PartialTypes._
import zio.Chunk

object ResponseService {

  def getAcknowledgment(msg: CoapMessage): Chunk[Byte] =
    acknowledgeMessage(msg.header.coapId).toByteChunk

  def getResetMessage(msg: IgnoredMessageWithId): Chunk[Byte] =
    resetMessage(msg._2).toByteChunk

  def getResetMessage(id: CoapId): Chunk[Byte] =
    resetMessage(id).toByteChunk

  private def resetMessage(id: CoapId): CoapMessage =
    CoapMessage.asResetWith(id)

  private def acknowledgeMessage(id: CoapId): CoapMessage =
    CoapMessage.asAckWith(id)
}
