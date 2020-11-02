package infrastructure.persistance


import domain.api.ResponseService._
import domain.api._
import domain.model.broker.BrokerRepository._
import domain.model.broker._
import domain.model.chunkstream.ChunkStreamRepository.Channel
import domain.model.chunkstream._
import domain.model.coap._
import domain.model.coap.header.fields._
import domain.model.config.ConfigRepository
import domain.model.exception.UnsharablePayload.UnsharablePayload
import domain.model.exception._
import domain.model.sender.MessageSenderRepository
import domain.model.sender.MessageSenderRepository._
import utility.PartialTypes._
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


  private def operation(element: (Option[SocketAddress], Chunk[Byte])) =
    CoapDeserializerService.parseCoapMessageWithoutErr(element._2).flatMap {
      case Left(id) => attemptReset(element._1, id)
      case Right(m) => attemptAcknowledgment(element._1, m) *> attemptPublish(m)
    }.ignore


  // TODO: MessageSenderRepository needs a service!
  private def attemptReset(address: Option[SocketAddress], id: Option[CoapId]) =
    ((address, id) match {
      case (Some(addr), Some(id)) => MessageSenderRepository.sendMessage(addr, ResponseService.getResetMessage(id))
      case (None, _)              => IO.fail(MissingAddress)
      case (Some(_), None)        => IO.fail(MissingCoapId)
    }).ignore

  private def attemptAcknowledgment(address: Option[SocketAddress], msg: CoapMessage) =
    (address match {
      case None       => IO.fail(MissingAddress)
      case Some(addr) =>
        IO.cond(msg.isConfirmable, ResponseService.getAckMessage(msg), NoResponseAvailable) >>= (sendMessage(addr, _))
    }).ignore

  private def attemptPublish(m: CoapMessage): URIO[BrokerRepository[PublisherResponse], Unit] =
    (for {
      _       <- isPut(m)
      path    <- m.getPath
      content <- m.getContent
      _       <- BrokerRepository.pushMessageTo(path, PublisherResponse.from(path, content))
    } yield ()).ignore

  private def isPut(msg: CoapMessage): IO[UnsharablePayload, Unit] =
    IO.cond(
      msg.header.coapCodePrefix.value == 0 && msg.header.coapCodeSuffix.value == 3,
      (),
      UnsharablePayload)
}

