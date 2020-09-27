package domain.model.broker

import domain.model.RouteModel.Route
import domain.model.exception.GatewayError
import subgrpc.subscription.PublisherResponse
import zio.stm.TQueue
import zio.{Has, IO, NonEmptyChunk, UIO, URIO, ZIO}

object BrokerRepository {

  type BrokerRepository = Has[BrokerRepository.Service]

  trait Service {
    def addTopic(uriPath: NonEmptyChunk[String]): UIO[Unit]
    def getQueue(id: Long): IO[Option[Nothing], TQueue[PublisherResponse]]
    def pushMessageTo(uriPath: NonEmptyChunk[String], msg: PublisherResponse): UIO[Unit]

    val getId: UIO[Long]
  }

  def addTopic(uriPath: NonEmptyChunk[String]): URIO[BrokerRepository, Unit] =
    ZIO.accessM(_.get.addTopic(uriPath))

  def getQueue(id: Long): ZIO[BrokerRepository, Option[Nothing], TQueue[PublisherResponse]] =
    ZIO.accessM(_.get.getQueue(id))

  def pushMessageTo(uriPath: NonEmptyChunk[String], msg: PublisherResponse): URIO[BrokerRepository, Unit] =
    ZIO.accessM(_.get.pushMessageTo(uriPath, msg))

  val getId: URIO[BrokerRepository, Long] =
    ZIO.accessM(_.get.getId)

}