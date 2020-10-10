package domain.model.broker


import domain.model.exception.MissingBrokerBucket.MissingBrokerBucket
import domain.model.exception.MissingSubscriber.MissingSubscriber
import subgrpc.subscription.PublisherResponse
import zio._
import zio.stm.TQueue

object BrokerRepository {

  type BrokerRepository[R] = Has[BrokerRepository.Service[R]]

  trait Service[R] {
    def addTopic(uriPath: NonEmptyChunk[String]): UIO[Unit]
    def getQueue(id: Long): IO[MissingBrokerBucket, TQueue[R]]
    def pushMessageTo(uriPath: Segments, msg: R): UIO[Unit]
    def addSubscriberTo(topics: Paths, id: Long): UIO[Unit]
    def removeSubscriber(id: Long): IO[MissingSubscriber, Unit]
    def getSubscribers(topic: String): UIO[Option[Set[Long]]]
    def removeSubscriptions(topics: Paths, id: Long): UIO[Unit]
    def getNextId: UIO[Long]
  }

  /**
   * The equivalent of one or many URI paths which consist of one or many segments.
   */
  type Paths    = NonEmptyChunk[NonEmptyChunk[String]]
  /**
   * One or many segments of an URI path.
   */
  type Segments = NonEmptyChunk[String]

  def addTopic[R: Tag](uriPath: NonEmptyChunk[String]): URIO[BrokerRepository[R], Unit] =
    ZIO.accessM(_.get.addTopic(uriPath))

  def getQueue[R: Tag](id: Long): ZIO[BrokerRepository[R], MissingBrokerBucket, TQueue[R]] =
    ZIO.accessM(_.get.getQueue(id))

  def pushMessageTo[R: Tag](uriPath: Segments, msg: R): URIO[BrokerRepository[R], Unit] =
    ZIO.accessM(_.get.pushMessageTo(uriPath, msg))

  def addSubscriberTo[R: Tag](topics: Paths, id: Long): URIO[BrokerRepository[R], Unit] =
    ZIO.accessM(_.get.addSubscriberTo(topics, id))

  def removeSubscriber[R: Tag](id: Long): ZIO[BrokerRepository[R], MissingSubscriber, Unit] =
    ZIO.accessM(_.get.removeSubscriber(id))

  def getSubscribers[R: Tag](topic: String): URIO[BrokerRepository[R], Option[Set[Long]]] =
    ZIO.accessM(_.get.getSubscribers(topic))

  def removeSubscriptions[R: Tag](topics: Paths, id: Long): URIO[BrokerRepository[R], Unit] =
    ZIO.accessM(_.get.removeSubscriptions(topics, id))

  def getNextId[R: Tag]: URIO[BrokerRepository[R], Long] =
    ZIO.accessM(_.get.getNextId)
}