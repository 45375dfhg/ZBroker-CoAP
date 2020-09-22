
import domain.model.RouteModel.Route
import zio.{Chunk, NonEmptyChunk, UIO, ZIO}
import zio.stm._


object Broker {

  private val subscriptions = TMap.empty[Route, Set[String]]

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
   * @return The newly merged set of subscribers for the given ID
   */
  def addSubscriber(uriRoute: NonEmptyChunk[Route], id: String): UIO[Set[String]] = {
    val key = buildRoute(uriRoute)
    subscriptions.flatMap(_.merge(key, Set(id))(_ union _)).commit
  }

  /**
   * Deletes a single subscriber from the value set of the given key (route). The removal is not applied to sub-routes.
   * @param uriRoute a split representation of the uri-path
   * @param id the id of the subscriber, used as a key for the hashmap
   */
  def deleteSubscriberFrom(uriRoute: NonEmptyChunk[Route], id: String): UIO[Unit] = {
    val key = buildRoute(uriRoute)
    subscriptions.flatMap(subs => STM.whenM(subs.contains(key))(subs.merge(key, Set(id))(_ diff _))).commit
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