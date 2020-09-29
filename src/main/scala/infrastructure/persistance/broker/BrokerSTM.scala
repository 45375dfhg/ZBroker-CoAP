package infrastructure.persistance.broker

import domain.model.broker.BrokerRepository
import infrastructure.persistance.broker.BrokerSTM.getSubRoutesFrom
import subgrpc.subscription.{Path, PublisherResponse}
import zio.stm._
import zio._


class BrokerSTM(
  val buckets: TMap[Long, TQueue[PublisherResponse]],
  val subscriptions: TMap[String, Set[Long]],
  val counter: TRef[Long]
) {
  def addSubscriberTo(topics: Seq[Path], id: Long): UIO[Unit] =
    STM.atomically {
      for {
        _    <- STM.unlessM(buckets.contains(id))(TQueue.unbounded[PublisherResponse] >>= (buckets.put(id, _)))
        keys =  topics.flatMap(path => BrokerSTM.getSubRoutesFrom(path.segments))
        _    <- STM.foreach_(keys)(key => subscriptions.merge(key, Set(id))(_ union _))
      } yield ()
    }

  def getQueue(id: Long): IO[Option[Nothing], TQueue[PublisherResponse]] =
    buckets.get(id).flatMap(STM.fromOption(_)).commit
}

// TODO: just remove the repository at this point ...
object BrokerSTM extends BrokerRepository.Service {

  private def getSubRoutesFrom(segments: Seq[String]): Seq[String] =
    segments.scanLeft("")(_ + _).drop(1)

  def make: STM[Nothing, BrokerSTM] =
    for {
      buckets <- TMap.empty[Long, TQueue[PublisherResponse]]
      subs    <- TMap.empty[String, Set[Long]]
      counter <- TRef.make(0L)
      repo    =  new BrokerSTM(buckets, subs, counter)
    } yield repo


//  private val subscriptions = TMap.empty[String, Set[Long]]
//
//  private val buckets = TMap.empty[Long, TQueue[PublisherResponse]]
//
//  private val uid = TRef.make(0L)
//
//  val getId: UIO[Long] = uid.flatMap(_.updateAndGet(_ + 1)).commit
//
//  def pushMessageTo(uriPath: NonEmptyChunk[String], msg: PublisherResponse): UIO[Unit] = {
//    val routes = getSubRoutesFrom(uriPath)
//
//    STM.atomically {
//      for {
//        sMap <- subscriptions
//        set  <- STM.foreach(routes)(route => sMap.getOrElse(route, Set.empty[Long])).map(_.reduce(_ union _))
//        qMap <- buckets
//        _    <- STM.foreach_(set) { key =>
//          for {
//            queueM <- qMap.get(key)
//            queue  <- queueM.fold(TQueue.unbounded[PublisherResponse])(STM.succeed(_))
//            _      <- queue.offer(msg)
//          } yield ()
//        }
//      } yield ()
//    }
//  }
//
//  def addSubscriberTo(topics: Seq[Path], id: Long): UIO[Unit] = {
//    val keys = topics.flatMap(path => getSubRoutesFrom(path.segments))
//
//    STM.atomically {
//      for {
//        qMap <- buckets
//        _    <- STM.unlessM(qMap.contains(id))(TQueue.unbounded[PublisherResponse] >>= (qMap.put(id, _)))
//        sMap <- subscriptions
//        _    <- STM.foreach_(keys)(key => sMap.merge(key, Set(id))(_ union _))
//      } yield ()
//    }
//  }
//
//  def removeSubscriber(topics: Seq[Path], id: Long): UIO[Unit] = {
//    val keys = topics.flatMap(path => getSubRoutesFrom(path.segments))
//
//    STM.atomically {
//      for {
//        sMap <- subscriptions
//        _    <- STM.foreach_(keys) { key =>
//                  STM.whenM(sMap.get(key).map(_.fold(false)(_.contains(id)))) {
//                    sMap.merge(key, Set(id))(_ diff _)
//                  }
//                }
//      } yield ()
//    }
//  }
//
//  def getQueue(id: Long): IO[Option[Nothing], TQueue[PublisherResponse]] = {
//    STM.atomically {
//      buckets.flatMap(map => map.get(id).flatMap(o => STM.fromOption(o)))
//    }
//  }
//
//  // TODO: Is this really necessary?
//  def addTopic(uriPath: NonEmptyChunk[String]): UIO[Unit] =
//    (getSubRoutesFrom _ andThen writeTopic)(uriPath)
//
//  private val writeTopic: Seq[String] => UIO[Unit] =
//    (keys: Seq[String]) =>
//      STM.atomically {
//        for {
//          sMap <- subscriptions
//          _    <- STM.foreach_(keys)(key => sMap.putIfAbsent(key, Set.empty[Long]))
//        } yield ()
//      }
//
//  private def getSubRoutesFrom(segments: Seq[String]): Seq[String] =
//    segments.scanLeft("")(_ + _).drop(1)
//
//  private def buildRoute(uriPath: NonEmptyChunk[String]): String =
//    uriPath.mkString

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

