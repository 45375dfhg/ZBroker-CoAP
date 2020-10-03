package infrastructure.persistance


import domain.api.ResponseService._
import domain.api._
import domain.model.broker.BrokerRepository._
import domain.model.broker._
import domain.model.chunkstream._
import domain.model.coap._
import domain.model.coap.header._
import domain.model.config.ConfigRepository
import domain.model.exception._
import domain.model.sender.MessageSenderRepository._

import utility.PartialTypes._
import utility.PublisherResponseExtension._

import subgrpc.subscription.PublisherResponse

import zio._
import zio.nio.core._

object PublisherServer {

  /**
   * The fire and forget start method of the PublisherServer
   */
  val make =
    for {
      n <- ConfigRepository.getStreamFiberAmount
      _ <- ChunkStreamRepository.getChunkStream.mapMParUnordered(n.value)(serverRoutine).runDrain
    } yield ()

  /**
   * The core method which is applied to each element on the incoming publisher stream.
   * @param streamChunk A tuple which might contain a SocketAddress and definitely contains a Chunk[Byte].
   */
  private def serverRoutine(streamChunk: (Option[SocketAddress], Chunk[Byte])) =
    (UIO.succeed(streamChunk._1) <*> CoapMessage.fromChunk(streamChunk._2)) // TODO: REFACTOR THE EITHER PART
      .collect(MissingCoapId)(messagesAndErrorsWithId).tap(sendReset)
      .collect(InvalidCoapMessage)(validMessage).tap(sendAcknowledgment) // TODO: ADD PIGGYBACKING BASED ON REQUEST PARAMS
      .map(isolateMessage).collect(UnsharablePayload)(sendableMessage)
      .tap(publishMessage)
      .ignore

  /**
   * Publishes the given CoapMessage on the layered Broker.
   */
  private def publishMessage(m: CoapMessage): URIO[BrokerRepository, Unit] =
    (for {
      path    <- m.getPath
      content <- m.getContent
      _       <- BrokerRepository.pushMessageTo(path, PublisherResponse.from(path, content))
    } yield ()).ignore

  /**
   * This function attempts to derive a SocketAddress from the Option, checks whether the provided message is
   * to be confirmed and if all attempts and checks are successful, it  sends an acknowledging message to the given address.
   * If any attempt or check fails this function will short-circuit and return an error which will be ignored.
   */
  private def sendAcknowledgment(streamChunk: (Option[SocketAddress], CoapMessage)) =
    streamChunk match {
      case (address, msg) => {
        IO.fromOption(address).orElseFail(MissingAddress) <*>
          IO.cond(msg.isConfirmable, getAcknowledgment(msg), NoResponseAvailable) >>=
            { case (address, acknowledgment) => sendMessage(address, acknowledgment) }
      }.ignore
    }

  /**
   * If the CoapMessage was unsuccessfully parsed but contains a CoapId and the origin is known, this function
   * will send a reset message to the message's origin. If the parsing was successful or the address is missing,
   * this function will do nothing.
   * @param streamChunk A tuple that might contain a SocketAddress and will either contain a CoapMessage or
   *                    its parsing error combined with the recovered CoapId.
   */
  private def sendReset(streamChunk: (Option[SocketAddress], Either[(MessageFormatError, CoapId), CoapMessage])) =
    streamChunk match {
      case (address, either) => either match {
        case Left(value) =>
          val msg = ResponseService.getResetMessage(value)
          IO.fromOption(address).orElseFail(MissingAddress).flatMap(sendMessage(_, msg)).ignore
        case Right(_)    => UIO.unit
      }
    }

  /**
   * Extracts the CoapMessage from the streamChunk tuple and thus drops the SocketAddress.
   */
  private def isolateMessage(tuple: (Option[SocketAddress], CoapMessage)) =
    tuple._2

  /**
   * Drops all CoapMessages that do not contain the "PUT" code.
   * */
  private def sendableMessage: PartialValidMessage = {
    case msg @ CoapMessage(header, _) if header.cPrefix.value == 0 && header.cSuffix.value == 3 => msg
  }

  /**
   * Drops all unsuccessfully parsed CoapMessages.
   */
  private def validMessage: PartialGatherMessages = {
    case (address, Right(message)) => (address, message)
  }

  /**
   * Drops all unsuccessfully parsed CoapMessages that do not contain a recovered CoAP id.
   */
  private def messagesAndErrorsWithId: PartialRemoveEmptyID = {
    case (address, Right(value))                    => (address, Right(value))
    case (address, Left((err, id))) if id.isDefined => (address, Left(err, id.get))
  }

}

