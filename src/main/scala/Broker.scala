
import domain.model.RouteModel.Route
import zio.{Chunk, NonEmptyChunk, UIO}
import zio.stm._
import zio.Queue


object Broker {

  private val subscriptions = TMap.empty[Route, Set[String]]

  // TODO: Setup a second map and implement a proper subscription id
  // private val queues = TMap.empty[String, Queue[String]]

  /**
   * Constructs entries for the route and its sub-routes
   * @param uriRoute a split representation of the uri-path
   */
  def addTopic(uriRoute: NonEmptyChunk[Route]): UIO[Unit] =
    (getSubRoutes _ andThen writeTopic)(uriRoute)

  /**
   * Recursively collects all subscribers for a given route and its sub-routes recursively while also constructing
   * topics for the route and its sub-routes if not already existent
   * @param uriRoute a split representation of the uri-path
   * @return A set of all subscribers that subscribed to a sub-route of the given route including the full-route
   */
  def getSubscribersAndAddTopics(uriRoute: NonEmptyChunk[Route]): UIO[Set[String]] =
    (getSubRoutes _ andThen readSubscribersAndWriteTopics)(uriRoute)

  /**
   * Gets all subscribers recursively for a given route which means that sub-routes and their respective
   * subscribers are included in the result set.
   * @param uriRoute a split representation of the uri-path
   * @return A set of all subscribers that subscribed to a sub-route of the given route including the full-route
   */
  def getSubscribers(uriRoute: NonEmptyChunk[Route]): UIO[Set[String]] =
    (getSubRoutes _ andThen readSubscribers)(uriRoute)

  /**
   * Writes a single subscriber to the given key
   * @param uriRoute a split representation of the uri-path
   * @param id the id of the subscriber, used as a key for the hashmap
   * @return The success of adding the subscriber as Boolean - success is mapped to true and vice versa
   */
  def addSubscriber(uriRoute: NonEmptyChunk[Route], id: String): UIO[Boolean] = {
    val key = buildRoute(uriRoute)
    STM.atomically {
      for {
        subs <- subscriptions
        bool <- subs.get(key).flatMap(o => STM.fromOption(o)).map(_.contains(id)).fold(_ => false, identity)
        _    <- STM.unless(bool)(subs.merge(key, Set(id))(_ union _))
      } yield bool
    }
  }

  /**
   * Deletes a single subscriber from the value set of the given key (route). The removal is not applied to sub-routes.
   * @param uriRoute a split representation of the uri-path
   * @param id the id of the subscriber, used as a key for the hashmap
   * @return A Boolean that reflects whether the deletion was successful or not.
   */
  def deleteSubscriberFrom(uriRoute: NonEmptyChunk[Route], id: String): UIO[Boolean] = {
    val key = buildRoute(uriRoute)
    STM.atomically {
      for {
        subs <- subscriptions
        bool <- subs.contains(key)
        _    <- STM.when(bool)(subs.merge(key, Set(id))(_ diff _))
      } yield bool
    }
  }

  val getAllTopics: UIO[List[Route]] =
    subscriptions.flatMap(_.keys).commit

  private val writeTopic: Chunk[Route] => UIO[Unit] =
    (keys: Chunk[Route]) =>
      STM.atomically {
        for {
          map <- subscriptions
          _   <- STM.foreach_(keys)(key => STM.succeed(map.merge(key, Set.empty[String])(_ union _)))
        } yield ()
      }

  private val readSubscribers: Chunk[Route] => UIO[Set[String]] =
    (keys: Chunk[Route]) =>
      STM.atomically {
        for {
          subs <- subscriptions
          sets <- STM.foreach(keys)(key => subs.getOrElse(key, Set.empty[String]))
        } yield sets.fold(Set.empty[String])(_ union _)
      }

  private val readSubscribersAndWriteTopics: Chunk[Route] => UIO[Set[String]] =
    (keys: Chunk[Route]) =>
      STM.atomically {
        for {
          subs <- subscriptions
          sets <- STM.foreach(keys)(key => subs.merge(key, Set[String]())(_ union _))
        } yield sets.fold(Set.empty[String])(_ union _)
      }

  private def getSubRoutes(uriRoute: NonEmptyChunk[Route]): Chunk[Route] =
    uriRoute.scan(Route(""))((acc, c) => Route(acc.asString + c.asString)).drop(1)

  private def buildRoute(uriRoute: NonEmptyChunk[Route]): Route =
    Route(uriRoute.mkString)
}