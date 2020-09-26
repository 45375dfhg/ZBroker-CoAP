package domain.model.broker

import domain.model.RouteModel.Route
import subgrpc.subscription.Path
import zio.{Has, NonEmptyChunk, UIO, URIO, ZIO}

object BrokerRepository {

  type BrokerRepository = Has[BrokerRepository.Service]

  trait Service {
    def addTopic(path: Path): UIO[Unit]
  }

  def addTopic(path: Path): URIO[BrokerRepository, Unit] =
    ZIO.accessM(_.get.addTopic(path))

}