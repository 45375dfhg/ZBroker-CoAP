package infrastructure.persistance.broker

import domain.model.broker.BrokerRepository
import domain.model.broker.BrokerRepository._
import domain.model.exception.MissingBrokerBucket
import domain.model.exception.MissingBrokerBucket._

import subgrpc.subscription.PublisherResponse
import zio.stm._
import zio._

/**
 * A broker which handles topics and subscribers. Topics are paired with their subscribers and
 * each subscriber has its own mail system which is a queue.
 * @param buckets A thread-safe bucket system for each incoming connection represented as a
 *                Long where the connection is paired with its own queue to push messages into.
 * @param subscriptions A thread-safe subscriber system where the key represents a topic and
 *                      its subscribers are saved inside the paired Set. A subscriber is
 *                      identified by its unique Long value.
 * @param counter A thread-safe counter that acts as supplier for unique connection ID's.
 */
class TransactionalBroker (
  val buckets: TMap[Long, TQueue[PublisherResponse]],
  val subscriptions: TMap[String, Set[Long]],
  val counter: TRef[Long]
) extends BrokerRepository.Service {

  /**
   * Increments and then returns the next ID while being thread-safe.
   */
  val getNextId: UIO[Long] = counter.updateAndGet(_ + 1).commit

  /**
   * Maps a new subscriber to one or multiple topics.
   * @param topics A sequence of topics that the user wants to subscribe to.
   * @param id The connection id of the user.
   */
  def addSubscriberTo(topics: Paths, id: Long): UIO[Unit] =
    STM.atomically {
      for {
        _    <- STM.unlessM(buckets.contains(id))(TQueue.unbounded[PublisherResponse] >>= (buckets.put(id, _)))
        keys =  topics.map(path => TransactionalBroker.buildRoute(path))
        _    <- STM.foreach_(keys)(key => subscriptions.merge(key, Set(id))(_ union _))
      } yield ()
    }

  // TODO: REWRITE THIS PROPERLY TO AN EITHER
  def getSubscribers(topic: String): UIO[Set[Long]] =
    STM.atomically {
      for {
        r <- subscriptions.getOrElse(topic, Set(666L))
      } yield r
    }

  /**
   * Attempts the mailbox (a queue) mapped to the specified ID.
   * WARNING: MULTIPLE EXTRACTIONS AND CONSUMPTIONS ARE NOT CHECKED FOR.
   * @param id The connection ID, used as a key value to get the queue.
   * @return Either a TQueue as planned or an UnexpectedError which represents a very faulty system state.
   */
  def getQueue(id: Long): IO[MissingBrokerBucket, TQueue[PublisherResponse]] = {
    STM.atomically {
      for {
        bool  <- buckets.contains(id)
        _     <- if (bool) STM.unit else STM.retry
        queue <- buckets.get(id).flatMap(STM.fromOption(_)).mapError(_ => MissingBrokerBucket)
      } yield queue
    }
  }

  /**
   * Pushes a message to all its related topic's subscribers by acquiring all subscribers from
   * all sub-routes that result from the given route. Then those subscribers are merged these
   * into a set to avoid duplicate messages. The message is then offered to each
   * subscribers personal queue.
   * @param uriPath An URI path which represents the topic to which the message is addressed.
   * @param msg The message - already converted into the PublisherResponse format.
   */
  def pushMessageTo(uriPath: Segments, msg: PublisherResponse): UIO[Unit] =
    STM.atomically {
      for {
        routes <- STM.succeed(TransactionalBroker.getSubRoutesFrom(uriPath))
        set    <- STM.foreach(routes)(route => subscriptions.getOrElse(route, Set.empty[Long])).map(_.reduce(_ union _))
        _      <- STM.foreach_(set) { key =>
                    for {
                      queueM <- buckets.get(key)
                      queue  <- queueM.fold(TQueue.unbounded[PublisherResponse])(STM.succeed(_))
                      _      <- queue.offer(msg)
                    } yield ()
                  }
      } yield ()
    }

  /**
   * Adds a URI path and it's sub-routes to the TransactionalBroker.
   */
  def addTopic(uriPath: Segments): UIO[Unit] =
    STM.atomically {
      for {
        keys <- STM.succeed(TransactionalBroker.getSubRoutesFrom(uriPath))
        _    <- STM.foreach_(keys)(key => subscriptions.putIfAbsent(key, Set.empty[Long]))
      } yield ()
    }

  /**TODO: WARNING: NOT A FINAL IMPLEMENTATION! THIS WILL LEAD TO INCONSISTENT BEHAVIOUR AND FRAGMENTS!
   * Removes a subscriber by its ID from one or more topics.
   * @param topics A sequence of topics
   * @param id The unique ID of a subscriber
   */
  def removeSubscriber(topics: Paths, id: Long): UIO[Unit] =
    STM.atomically {
      for {
        keys <- STM.succeed(topics.map(path => TransactionalBroker.buildRoute(path)))
        _    <- STM.foreach_(keys) { key =>
                  STM.whenM(subscriptions.get(key).map(_.fold(false)(_.contains(id)))) {
                    subscriptions.merge(key, Set(id))(_ diff _)
                  }
                }
      } yield ()
    }
}

object TransactionalBroker {

  private def getSubRoutesFrom(segments: Segments): Seq[String] =
    segments.scanLeft("")(_ + _).tail

  private def buildRoute(uriPath: Segments): String =
    uriPath.mkString

  def make: STM[Nothing, TransactionalBroker] =
    for {
      buckets <- TMap.empty[Long, TQueue[PublisherResponse]]
      subs    <- TMap.empty[String, Set[Long]]
      counter <- TRef.make(0L)
      repo    =  new TransactionalBroker(buckets, subs, counter)
    } yield repo

}
