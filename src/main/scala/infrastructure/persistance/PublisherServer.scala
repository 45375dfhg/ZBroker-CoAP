package infrastructure.persistance

import domain.api._
import domain.model.broker.BrokerRepository._
import domain.model.broker._
import domain.model.chunkstream._
import domain.model.coap._
import domain.model.coap.header.fields._
import domain.model.config.ConfigRepository
import domain.model.exception.UnsharablePayload.UnsharablePayload
import domain.model.exception._
import domain.model.sender.MessageSenderRepository
import domain.model.sender.MessageSenderRepository._
import utility.classExtension.PublisherResponseExtension._
import subgrpc.subscription.PublisherResponse
import zio._
import zio.console._
import zio.nio.core._

object PublisherServer {

  /**
   * The fire and forget start method of the PublisherServer
   */
  val make =
    for {
      n <- ConfigRepository.getStreamFiberAmount <* putStrLn("[PUB] PublisherServer  loading Config. Starting ...")
      _ <- ChunkStreamRepository.getChunkStream.mapMParUnordered(n.value)(operation).runDrain
    } yield ()

  /**
   * Entry point that depending on tuple will attempt to send a reset message or attempt to send
   * an acknowledgment message and then attempt to publish the content
   * @param element a tuple based on the origins address and the received datagram
   */
  private def operation(element: (Option[SocketAddress], Chunk[Byte])) =
    CoapDeserializerService.parseCoapMessageWithoutErr(element._2).flatMap {
      case Left(id) => attemptReset(element._1, id)
      case Right(m) => attemptAcknowledgment(element._1, m) *> attemptPublish(m)
    }.ignore

  // TODO: MessageSenderRepository needs a service!
  /**
   * Attempts to send a reset message if the origin's socket address is defined and a message id was
   * successfully recovered from the datagram - else the respective successful error is returned
   */
  private def attemptReset(address: Option[SocketAddress], id: Option[CoapId]) =
    ((address, id) match {
      case (Some(addr), Some(id)) => MessageSenderRepository.sendMessage(addr, ResponseService.getResetMessage(id))
      case (None, _)              => IO.fail(MissingAddress)
      case (Some(_), None)        => IO.fail(MissingCoapId)
    }).ignore

  /**
   * Attempts to send an acknowledgment message to the origin if it is defined and the origin requires a response,
   * otherwise the respective successful error is returned
   */
  private def attemptAcknowledgment(address: Option[SocketAddress], msg: CoapMessage) =
    (address match {
      case None       => IO.fail(MissingAddress)
      case Some(addr) =>
        IO.cond(msg.isConfirmable, ResponseService.getAckMessage(msg), NoResponseAvailable) >>= (sendMessage(addr, _))
    }).ignore

  /**
   * If a message has a PUT code, an URI-path and a payload, the message will be published on the broker
   */
  private def attemptPublish(m: CoapMessage): URIO[BrokerRepository[PublisherResponse], Unit] =
    (for {
      _       <- hasPutMessageCode(m)
      path    <- m.getPath
      content <- m.getContent
      _       <- BrokerRepository.pushMessageTo(path, PublisherResponse.from(path, content))
    } yield ()).ignore

  /**
   * Checks the header of a message for its code - if the code is 0:3 (PUT) it returns true
   */
  private def hasPutMessageCode(msg: CoapMessage): IO[UnsharablePayload, Unit] =
    IO.cond(
      msg.header.coapCodePrefix.value == 0 && msg.header.coapCodeSuffix.value == 3,
      (),
      UnsharablePayload)
}

