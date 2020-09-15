package domain.api

import domain.model.coap.header._
import domain.model.coap._
import zio.Chunk

object ResponseService {

  val initialResponse: Either[(MessageFormatError, CoapId), CoapMessage] => Option[Chunk[Byte]] =
    deriveResponse _ andThen convertResponse

  val piggyResponse = ???

  // TODO: NEED TO PIGGYBACK THE ACTUAL RESPONSE! // TODO: do some logging for the error?
  private def deriveResponse(msgE: Either[(MessageFormatError, CoapId), CoapMessage]): Option[CoapMessage] =
    msgE match {
      case Right(message) if message.isConfirmable => Some(acknowledgeMessage(message.header.msgID))
      case Left((_, id))                           => Some(resetMessage(id))
      case _                                       => None
    }

  private def convertResponse(msg: Option[CoapMessage]): Option[Chunk[Byte]] =
    msg.map(CoapSerializerService.generateFromMessage)

  private def resetMessage(id: CoapId): CoapMessage =
    CoapMessage(CoapHeader.reset(id), CoapBody.empty)

  private def acknowledgeMessage(id: CoapId): CoapMessage =
    CoapMessage(CoapHeader.ack(id), CoapBody.empty)
}
