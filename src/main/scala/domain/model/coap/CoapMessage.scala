package domain.model.coap

import domain.model.coap.header.CoapId

final case class CoapMessage(header: CoapHeader, body: CoapBody) {
  def isConfirmable: Boolean    = this.header.msgType.value == 0
  def isNonConfirmable: Boolean = this.header.msgType.value == 1
}

object CoapMessage {
  def reset(id : CoapId) = CoapMessage(CoapHeader.reset(id), CoapBody.empty)
  def ack(id   : CoapId) = CoapMessage(CoapHeader.ack(id), CoapBody.empty)
}







