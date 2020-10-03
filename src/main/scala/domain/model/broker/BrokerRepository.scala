package domain.model.broker


import domain.model.exception.MissingBrokerBucket.MissingBrokerBucket
import domain.model.exception.MissingSubscriber.MissingSubscriber
import subgrpc.subscription.PublisherResponse
import zio._
import zio.stm.TQueue

object BrokerRepository {

  type BrokerRepository = Has[BrokerRepository.Service]

  trait Service {
    def addTopic(uriPath: NonEmptyChunk[String]): UIO[Unit]
    def getQueue(id: Long): IO[MissingBrokerBucket, TQueue[PublisherResponse]]
    def pushMessageTo(uriPath: Segments, msg: PublisherResponse): UIO[Unit]
    def addSubscriberTo(topics: Paths, id: Long): UIO[Unit]
    def removeSubscriber(id: Long): IO[MissingSubscriber, Unit]
    def getSubscribers(topic: String): UIO[Option[Set[Long]]]
    def removeSubscriptions(topics: Paths, id: Long): UIO[Unit]

    val getNextId: UIO[Long]
  }

  /**
   * The equivalent of one or many URI paths which consist of one or many segments.
   */
  type Paths    = NonEmptyChunk[NonEmptyChunk[String]]
  /**
   * One or many segments of an URI path.
   */
  type Segments = NonEmptyChunk[String]

  def addTopic(uriPath: NonEmptyChunk[String]): URIO[BrokerRepository, Unit] =
    ZIO.accessM(_.get.addTopic(uriPath))

  def getQueue(id: Long): ZIO[BrokerRepository, MissingBrokerBucket, TQueue[PublisherResponse]] =
    ZIO.accessM(_.get.getQueue(id))

  def pushMessageTo(uriPath: Segments, msg: PublisherResponse): URIO[BrokerRepository, Unit] =
    ZIO.accessM(_.get.pushMessageTo(uriPath, msg))

  def addSubscriberTo(topics: Paths, id: Long): URIO[BrokerRepository, Unit] =
    ZIO.accessM(_.get.addSubscriberTo(topics, id))

  def removeSubscriber(id: Long): ZIO[BrokerRepository, MissingSubscriber, Unit] =
    ZIO.accessM(_.get.removeSubscriber(id))

  def getSubscribers(topic: String): URIO[BrokerRepository, Option[Set[Long]]] =
    ZIO.accessM(_.get.getSubscribers(topic))

  def removeSubscriptions(topics: Paths, id: Long): URIO[BrokerRepository, Unit] =
    ZIO.accessM(_.get.removeSubscriptions(topics, id))

  val getNextId: URIO[BrokerRepository, Long] =
    ZIO.accessM(_.get.getNextId)
}