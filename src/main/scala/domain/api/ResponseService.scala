package domain.api

import domain.api.CoapDeserializerService.IgnoredMessage
import domain.model.coap.header._
import domain.model.coap._
import domain.model.exception.NoResponseAvailable
import domain.model.exception.NoResponseAvailable.NoResponseAvailable
import zio.{Chunk, IO}

object ResponseService {

  def getResponse(msg: Either[IgnoredMessage, CoapMessage]) =
    IO.fromEither((generateResponse _ andThen convertResponse) (msg))

  // TODO: NEED TO PIGGYBACK THE ACTUAL RESPONSE! // TODO: do some logging for the error?
  private def generateResponse(msg: Either[IgnoredMessage, CoapMessage]): Either[NoResponseAvailable, CoapMessage] =
    msg match {
      case Right(message) if message.isConfirmable => Right(acknowledgeMessage(message.header.msgID))
      case Left((_, id))                           => Right(resetMessage(id))
      case _                                       => Left(NoResponseAvailable)
    }

  def hasResponse(msg: Either[IgnoredMessage, CoapMessage]): Boolean =
    msg match {
      case Right(message) if message.isConfirmable => true
      case Left((_, _))                            => true
      case _                                       => false
    }

  private def convertResponse(msg: Either[NoResponseAvailable, CoapMessage]): Either[NoResponseAvailable, Chunk[Byte]] =
    msg.map(CoapSerializerService.generateFromMessage)

  private def resetMessage(id: CoapId): CoapMessage =
    CoapMessage.reset(id)

  private def acknowledgeMessage(id: CoapId): CoapMessage =
    CoapMessage.ack(id)
}
