package infrastructure.persistance.broker

import domain.model.RouteModel._
import domain.model.broker.BrokerRepository

import subgrpc.subscription.Path

import zio.stm._
import zio._

object BrokerSTM extends BrokerRepository.Service {

  private val subscriptions = TMap.empty[Route, Set[Long]]

  // TODO: Setup a second map and implement a proper subscription id
  private val buckets = TMap.empty[Long, TQueue[String]]

  def addTopic(path: Path): UIO[Unit] =
    (getSubRoutesFrom _ andThen writeTopic) (path)

  private val writeTopic: Seq[Route] => UIO[Unit] =
    (keys: Seq[Route]) =>
      STM.atomically {
        for {
          map <- subscriptions
          _   <- STM.foreach_(keys)(key => map.putIfAbsent(key, Set.empty[Long]))
        } yield ()
      }

  def addSubscriberTo(topics: Seq[Path], id: Long): UIO[Unit] = {
    val keys = topics.flatMap(getSubRoutesFrom)

    STM.atomically {
      for {
        qMap <- buckets
        _    <- STM.unlessM(qMap.contains(id))(STM.succeed(TQueue.unbounded[String] >>= (qMap.put(id, _))))
        sMap <- subscriptions
        _    <- STM.foreach_(keys)(key => sMap.merge(key, Set(id))(_ union _))
      } yield ()
    }
  }



  private def getSubRoutesFrom(path: Path): Seq[Route] =
    path.segments.scanLeft(Route(""))((acc, c) => Route(acc.asString + c))

  private def buildRoute(path: Path): Route =
    Route(path.segments.mkString)

}


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