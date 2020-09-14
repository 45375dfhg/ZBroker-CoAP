
import domain.api.CoapDeserializerService.IgnoredMessage

import domain.model.coap.{CoapBody, CoapHeader, CoapMessage, MessageFormatError}
import domain.model.coap.header._


object ResponseService {

  def deriveResponse(in: Either[(MessageFormatError, CoapId), CoapMessage]): Option[CoapMessage] = in match {
    case Right(message)  => if (message.isConfirmable) Some(acknowledgeMessage(message.header.msgID)) else None
    case Left((err, id)) => Some(resetMessage(id)) // do some logging for the error
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