package domain.model.broker

import domain.model.RouteModel.Route
import domain.model.exception.GatewayError

import zio.stm.TQueue
import zio.{Has, IO, NonEmptyChunk, UIO, URIO, ZIO}

object BrokerRepository {

  type BrokerRepository = Has[BrokerRepository.Service]

  trait Service {
    def addTopic(uriPath: NonEmptyChunk[Route]): UIO[Unit]
    def getQueue(id: Long): IO[Option[Nothing], TQueue[String]]
    val getId: UIO[Long]
  }

  def addTopic(uriPath: NonEmptyChunk[Route]): URIO[BrokerRepository, Unit] =
    ZIO.accessM(_.get.addTopic(uriPath))

  def getQueue(id: Long): ZIO[BrokerRepository, Option[Nothing], TQueue[String]] =
    ZIO.accessM(_.get.getQueue(id))

  val getId: URIO[BrokerRepository, Long] =
    ZIO.accessM(_.get.getId)

}