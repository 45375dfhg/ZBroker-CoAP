package domain.model.broker

import domain.model.exception.MissingBrokerBucket.MissingBrokerBucket
import subgrpc.subscription.{Path, PublisherResponse}
import zio.stm.TQueue
import zio.{Has, IO, NonEmptyChunk, UIO, URIO, ZIO}

object BrokerRepository {

  type BrokerRepository = Has[BrokerRepository.Service]

  trait Service {
    def addTopic(uriPath: NonEmptyChunk[String]): UIO[Unit]
    def getQueue(id: Long): IO[MissingBrokerBucket, TQueue[PublisherResponse]]
    def pushMessageTo(uriPath: NonEmptyChunk[String], msg: PublisherResponse): UIO[Unit]
    def addSubscriberTo(topics: Seq[Path], id: Long): UIO[Unit]
    def removeSubscriber(topics: Seq[Path], id: Long): UIO[Unit]

    val getNextId: UIO[Long]
  }

  def addTopic(uriPath: NonEmptyChunk[String]): URIO[BrokerRepository, Unit] =
    ZIO.accessM(_.get.addTopic(uriPath))

  def getQueue(id: Long): ZIO[BrokerRepository, MissingBrokerBucket, TQueue[PublisherResponse]] =
    ZIO.accessM(_.get.getQueue(id))

  def pushMessageTo(uriPath: NonEmptyChunk[String], msg: PublisherResponse): URIO[BrokerRepository, Unit] =
    ZIO.accessM(_.get.pushMessageTo(uriPath, msg))

  def addSubscriberTo(topics: Seq[Path], id: Long): URIO[BrokerRepository, Unit] =
    ZIO.accessM(_.get.addSubscriberTo(topics, id))

  def removeSubscriber(topics: Seq[Path], id: Long): URIO[BrokerRepository, Unit] =
    ZIO.accessM(_.get.removeSubscriber(topics, id))

  val getNextId: URIO[BrokerRepository, Long] =
    ZIO.accessM(_.get.getNextId)
}