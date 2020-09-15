package domain.api

import domain.api.CoapDeserializerService.IgnoredMessage
import domain.model.coap.header._
import domain.model.coap._
import domain.model.exception.{GatewayError, NoResponse}
import zio.{Chunk, IO}

object ResponseService {

  def getResponse(msg: Either[IgnoredMessage, CoapMessage]): IO[GatewayError, Chunk[Byte]] =
    IO.fromOption((deriveResponse _ andThen convertResponse) (msg)).orElseFail(NoResponse)

  val piggyResponse = ???

  // TODO: NEED TO PIGGYBACK THE ACTUAL RESPONSE! // TODO: do some logging for the error?
  private def deriveResponse(msgE: Either[IgnoredMessage, CoapMessage]): Option[CoapMessage] =
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
