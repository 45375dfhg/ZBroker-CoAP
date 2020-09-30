package infrastructure.persistance.broker

import domain.model.broker.BrokerRepository
import domain.model.exception.MissingBrokerBucket
import domain.model.exception.MissingBrokerBucket.MissingBrokerBucket
import subgrpc.subscription.{Path, PublisherResponse}
import zio.stm._
import zio._


class TransactionalBroker (
  val buckets: TMap[Long, TQueue[PublisherResponse]],
  val subscriptions: TMap[String, Set[Long]],
  val counter: TRef[Long]
) extends BrokerRepository.Service {

  val getNextId: UIO[Long] = counter.updateAndGet(_ + 1).commit

  def addSubscriberTo(topics: Seq[Path], id: Long): UIO[Unit] =
    STM.atomically {
      for {
        _    <- STM.unlessM(buckets.contains(id))(TQueue.unbounded[PublisherResponse] >>= (buckets.put(id, _)))
        keys =  topics.flatMap(path => TransactionalBroker.buildRoute(path.segments))
        _    <- STM.foreach_(keys)(key => subscriptions.merge(key, Set(id))(_ union _))
      } yield ()
    }

  def getQueue(id: Long): IO[MissingBrokerBucket, TQueue[PublisherResponse]] =
    buckets.get(id).flatMap(STM.fromOption(_)).mapError(_ => MissingBrokerBucket)commit

  def pushMessageTo(uriPath: NonEmptyChunk[String], msg: PublisherResponse): UIO[Unit] =
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

  def addTopic(uriPath: NonEmptyChunk[String]): UIO[Unit] =
    STM.atomically {
      for {
        keys <- STM.succeed(TransactionalBroker.getSubRoutesFrom(uriPath))
        _    <- STM.foreach_(keys)(key => subscriptions.putIfAbsent(key, Set.empty[Long]))
      } yield ()
    }

  def removeSubscriber(topics: Seq[Path], id: Long): UIO[Unit] =
    STM.atomically {
      for {
        keys <- STM.succeed(topics.flatMap(path => TransactionalBroker.getSubRoutesFrom(path.segments)))
        _    <- STM.foreach_(keys) { key =>
                  STM.whenM(subscriptions.get(key).map(_.fold(false)(_.contains(id)))) {
                    subscriptions.merge(key, Set(id))(_ diff _)
                  }
                }
      } yield ()
    }
}

object TransactionalBroker {

  private def getSubRoutesFrom(segments: Seq[String]): Seq[String] =
    segments.scanLeft("")(_ + _).tail

  def make: STM[Nothing, TransactionalBroker] =
    for {
      buckets <- TMap.empty[Long, TQueue[PublisherResponse]]
      subs    <- TMap.empty[String, Set[Long]]
      counter <- TRef.make(0L)
      repo    =  new TransactionalBroker(buckets, subs, counter)
    } yield repo

  private def buildRoute(uriPath: Seq[String]): String =
    uriPath.mkString

}

//  // TODO: Refactor the functions below to one single FN
//  private def getSubRoutes(uriRoute: NonEmptyChunk[String]): Chunk[String] =
//    uriRoute.scan("")(_ + _).drop(1)

//  private def getSubRoutes(uriRoute: NonEmptyChunk[Route]): Chunk[Route] =
//    uriRoute.scan(Route(""))((acc, c) => Route(acc.asString + c.asString)).drop(1)
//
//  private def buildRoute(uriRoute: NonEmptyChunk[Route]): Route =
//    Route(uriRoute.mkString)


//  /**
//   * Recursively collects all subscribers for a given route and its sub-routes recursively while also constructing
//   * topics for the route and its sub-routes if not already existent
//   *
//   * @param uriRoute a split representation of the uri-path
//   * @return A set of all subscribers that subscribed to a sub-route of the given route including the full-route
//   */
//  def getSubscribersAndAddTopics(uriRoute: NonEmptyChunk[Route]): UIO[Set[Long]] =
//    (getSubRoutes _ andThen readSubscribersAndWriteTopics) (uriRoute)
//
//  /**
//   * Gets all subscribers recursively for a given route which means that sub-routes and their respective
//   * subscribers are included in the result set.
//   *
//   * @param uriRoute a split representation of the uri-path
//   * @return A set of all subscribers that subscribed to a sub-route of the given route including the full-route
//   */
//  def getSubscribers(uriRoute: NonEmptyChunk[Route]): UIO[Set[Long]] =
//    (getSubRoutes _ andThen readSubscribers) (uriRoute)
//
//  /**
//   * Writes a single subscriber to the given key
//   *
//   * @param uriRoute a split representation of the uri-path
//   * @param id       the id of the subscriber, used as a key for the hashmap
//   * @return The success of adding the subscriber as Boolean - success is mapped to true and vice versa
//   */
//  def addSubscriber(uriRoute: NonEmptyChunk[Route], id: Long): UIO[Boolean] = {
//    val key = buildRoute(uriRoute)
//    STM.atomically {
//      for {
//        subs <- subscriptions
//        bool <- subs.get(key).flatMap(o => STM.fromOption(o)).map(_.contains(id)).fold(_ => false, identity)
//        _ <- STM.unless(bool)(subs.merge(key, Set(id))(_ union _))
//      } yield bool
//    }
//  }
//
//  /**
//   * Deletes a single subscriber from the value set of the given key (route). The removal is not applied to sub-routes.
//   *
//   * @param uriRoute a split representation of the uri-path
//   * @param id       the id of the subscriber, used as a key for the hashmap
//   * @return A Boolean that reflects whether the deletion was successful or not.
//   */
//  def deleteSubscriberFrom(uriRoute: NonEmptyChunk[Route], id: Long): UIO[Boolean] = {
//    val key = buildRoute(uriRoute)
//    STM.atomically {
//      for {
//        subs <- subscriptions
//        bool <- subs.contains(key)
//        _ <- STM.when(bool)(subs.merge(key, Set(id))(_ diff _))
//      } yield bool
//    }
//  }
//
//  val getAllTopics: UIO[List[Route]] =
//    subscriptions.flatMap(_.keys).commit
//

//
//  private val readSubscribers: Chunk[Route] => UIO[Set[Long]] =
//    (keys: Chunk[Route]) =>
//      STM.atomically {
//        for {
//          subs <- subscriptions
//          sets <- STM.foreach(keys)(key => subs.getOrElse(key, Set.empty[Long]))
//        } yield sets.fold(Set.empty[Long])(_ union _)
//      }
//
//  private val readSubscribersAndWriteTopics: Chunk[Route] => UIO[Set[Long]] =
//    (keys: Chunk[Route]) =>
//      STM.atomically {
//        for {
//          subs <- subscriptions
//          sets <- STM.foreach(keys)(key => subs.merge(key, Set[Long]())(_ union _))
//        } yield sets.fold(Set.empty[Long])(_ union _)
//      }

//  private def getSubRoutes(uriRoute: NonEmptyChunk[Route]): Chunk[Route] =
//    uriRoute.scan(Route(""))((acc, c) => Route(acc.asString + c.asString)).drop(1)
//
//  private def buildRoute(uriRoute: NonEmptyChunk[Route]): Route =
//    Route(uriRoute.mkString)




//  def addSubscriber(uriRoute: NonEmptyChunk[Route], id: Long): UIO[Boolean] = {
//    val key = buildRoute(uriRoute)
//    STM.atomically {
//      for {
//        subs <- subscriptions
//        bool <- subs.get(key).flatMap(o => STM.fromOption(o)).map(_.contains(id)).fold(_ => false, identity)
//        _ <- STM.unless(bool)(subs.merge(key, Set(id))(_ union _))
//      } yield bool
//    }
//  }

