package domain.api

import domain.model.coap.header._
import domain.model.coap._

object ResponseService {

  def deriveResponse(in: Either[(MessageFormatError, CoapId), CoapMessage]): Option[CoapMessage] = in match {
    // TODO: NEED TO PIGGYBACK THE ACTUAL RESPONSE!
    case Right(message)  => if (message.isConfirmable) Some(acknowledgeMessage(message.header.msgID)) else None
    case Left((_, id)) => Some(resetMessage(id)) // TODO: do some logging for the error?
  }

  def resetMessage(id: CoapId) =
    CoapMessage(
      CoapHeader(
        CoapVersion.default, CoapType.acknowledge, CoapTokenLength.empty, CoapCodePrefix.empty, CoapCodeSuffix.empty, id),
      CoapBody(None, None, None))

  def acknowledgeMessage(id: CoapId) =
    CoapMessage(
      CoapHeader(
        CoapVersion.default, CoapType.empty, CoapTokenLength.empty, CoapCodePrefix.empty, CoapCodeSuffix.empty, id),
      CoapBody(None, None, None))

}
