package domain.model.coap

final case class CoapMessage(header: CoapHeader, body: CoapBody) {
  def isConfirmable: Boolean    = this.header.msgType.value == 0
  def isNonConfirmable: Boolean = this.header.msgType.value == 1
}







