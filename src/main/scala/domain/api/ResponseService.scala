package domain.api

import domain.api.CoapDeserializerService.IgnoredMessageWithId
import domain.model.coap.header._
import domain.model.coap._
import domain.model.exception.{NoResponseAvailable, SuccessfulFailure}
import zio.{Chunk, IO}

object ResponseService {

//  def getResponse(msg: Either[IgnoredMessageWithId, CoapMessage]) =
//    IO.fromEither(generateResponse(msg).map(a => CoapSerializerService.generateFromMessage(a)))

  def getResponse(msg: CoapMessage) =
    CoapSerializerService.generateFromMessage(acknowledgeMessage(msg.header.msgID))


  def getResetMessage(msg: IgnoredMessageWithId): Chunk[Byte] =
    CoapSerializerService.generateFromMessage(resetMessage(msg._2))

  // TODO: NEED TO PIGGYBACK THE ACTUAL RESPONSE! // TODO: do some logging for the error?
  private def generateResponse(msg: Either[IgnoredMessageWithId, CoapMessage]): Either[SuccessfulFailure, CoapMessage] =
    msg match {
      case Right(message) if message.isConfirmable => Right(acknowledgeMessage(message.header.msgID))
      case Left((_, id))                           => Right(resetMessage(id))
      case _                                       => Left(NoResponseAvailable)
    }

  def hasResponse(msg: Either[IgnoredMessageWithId, CoapMessage]): Boolean =
    msg match {
      case Right(message) if message.isConfirmable => true
      case Left((_, _))                            => true
      case _                                       => false
    }

//  private def convertResponse(msg: CoapMessage): Either[SuccessfulFailure, Chunk[Byte]] =
//    CoapSerializerService.generateFromMessage(msg)

  private def resetMessage(id: CoapId): CoapMessage =
    CoapMessage.reset(id)

  private def acknowledgeMessage(id: CoapId): CoapMessage =
    CoapMessage.ack(id)
}
