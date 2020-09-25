package domain.model.broker

import domain.model.RouteModel.Route
import zio.{Has, NonEmptyChunk, UIO, URIO, ZIO}

object BrokerRepository {

  type BrokerRepository = Has[BrokerRepository.Service]

  trait Service {
    def addTopic(uriRoute: NonEmptyChunk[Route]): UIO[Unit]
  }

  def addTopic(uriRoute: NonEmptyChunk[Route]): URIO[BrokerRepository, Unit] =
    ZIO.accessM(_.get.addTopic(uriRoute))

}