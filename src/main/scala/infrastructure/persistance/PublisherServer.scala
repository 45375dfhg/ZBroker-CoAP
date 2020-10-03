package infrastructure.persistance

import domain.api.CoapDeserializerService.{IgnoredMessageWithId, IgnoredMessageWithIdOption}
import domain.api.ResponseService.getAcknowledgment
import domain.api._
import domain.model.broker._
import domain.model.chunkstream._
import domain.model.coap._
import domain.model.coap.header._
import domain.model.config.ConfigRepository
import domain.model.exception._
import domain.model.sender.MessageSenderRepository.sendMessage
import infrastructure.persistance.Controller.boot
import subgrpc.subscription.PublisherResponse
import utility.PublisherResponseExtension._
import zio.console._
import zio.nio.core.SocketAddress
import zio.stream._
import zio.{IO, UIO}



object PublisherServer {

  val make = {
    for {
      n <- ConfigRepository.getStreamFiberAmount

    } yield ()
    ChunkStreamRepository.getChunkStream.mapMParUnordered() { case (address, chunk) =>
      (UIO.succeed(address) <*> CoapMessage.fromChunk(chunk))
        .collect(MissingCoapId)(messagesAndErrorsWithId).tap(sendReset)
        .collect(InvalidCoapMessage)(validMessage).tap(sendAcknowledgment)
        .map(isolateMessage).collect(UnsharablePayload)(viableMessage)
        .tap(pushViableMessage)
        .ignore
    }.runDrain
  }

  // TODO - all these send and push messages are somehow part of a service or two - not part of the server mode itself!
  private def pushViableMessage(m: CoapMessage) = {
    (for {
      route   <- m.getPath
      content <- m.getContent
      _       <- BrokerRepository.pushMessageTo(route, PublisherResponse.from(route, content))
    } yield ()).orElseSucceed(())
  }

  private def sendAcknowledgment(tuple: (Option[SocketAddress], CoapMessage)) = {
    tuple match {
      case (address, msg) => {
        IO.fromOption(address).orElseFail(MissingAddress) <*>
          IO.cond(msg.isConfirmable, getAcknowledgment(msg), NoResponseAvailable) >>=
          (tuple => sendMessage(tuple._1, tuple._2))
      }.either
    }
  }

  private def viableMessage: PartialValidMessage = {
    case t @ CoapMessage(header, _) if header.cPrefix.value == 0 && header.cSuffix.value == 3 => t
  }

  private def isolateMessage(tuple: (Option[SocketAddress], CoapMessage)) =
    tuple._2

  private def sendReset(tuple: (Option[SocketAddress], Either[(MessageFormatError, CoapId), CoapMessage])) =
    tuple match {
      case (address, either) => either match {
        case Left(value) =>
          val msg = ResponseService.getResetMessage(value)
          IO.fromOption(address).orElseFail(MissingAddress).flatMap(sendMessage(_, msg)).either
        case Right(_)    => UIO.unit
      }
    }

  private def validMessage: PartialGatherMessages = {
    case (address, Right(message)) => (address, message)
  }

  private def messagesAndErrorsWithId: PartialRemoveEmptyID = {
    case (address, Right(value))                    => (address, Right(value))
    case (address, Left((err, id))) if id.isDefined => (address, Left(err, id.get))
  }

  type PartialValidMessage = PartialFunction[CoapMessage, CoapMessage]

  type PartialGatherMessages = PartialFunction[(Option[SocketAddress], Either[IgnoredMessageWithId, CoapMessage]),
    (Option[SocketAddress], CoapMessage)]

  type PartialRemoveEmptyID = PartialFunction[(Option[SocketAddress], Either[IgnoredMessageWithIdOption, CoapMessage]),
    (Option[SocketAddress], Either[IgnoredMessageWithId, CoapMessage])]

}

//ChunkStreamRepository
//  .getChunkStream
//  .mapM { case (address, chunk) => UIO(address) <*> extractFromChunk(chunk) } // TODO: REFACTOR THE EITHER PART
//  .collect(messagesAndErrorsWithId)
//  .tap(sendResets)
//  .collect(validMessage)
//  .tap(sendAcknowledgment) // TODO: ADD PIGGYBACKING BASED ON REQUEST PARAMS
//  .map(isolateMessage)
//  .foreach(pushViableMessage)