
import zio.{Chunk, IO, NonEmptyChunk, UIO}
import zio.stm._

object Broker {

  private val subscriptions = TMap.empty[String, Set[String]]

  /**
   * Constructs entries for the route and its sub-routes
   * @param route a split representation of the uri-path
   */
  def addTopic(route: NonEmptyChunk[String]): UIO[Unit] =
    (getSubRoutes _ andThen writeTopic)(route)

  /**
   * Recursively collects all subscribers for a given route and its sub-routes recursively while also constructing
   * topics for the route and its sub-routes if not already existent
   * @param route a split representation of the uri-path
   * @return A set of all subscribers that subscribed to a sub-route of the given route including the full-route
   */
  def getSubscribersAndAddTopics(route: NonEmptyChunk[String]): UIO[Set[String]] =
    (getSubRoutes _ andThen readSubscribersAndWriteTopics)(route)

  /**
   * Gets all subscribers recursively for a given route which means that sub-routes and their respective
   * subscribers are included in the result set.
   * @param route a split representation of the uri-path
   * @return A set of all subscribers that subscribed to a sub-route of the given route including the full-route
   */
  def getSubscribers(route: NonEmptyChunk[String]): UIO[Set[String]] =
    (getSubRoutes _ andThen readSubscribers)(route)

  /**
   * Writes a single subscriber to the given key
   * @param route a split representation of the uri-path
   * @param id the id of the subscriber, used as a key for the hashmap
   * @return The newly merged set of subscribers for the given ID
   */
  def addSubscriber(route: NonEmptyChunk[String], id: String): UIO[Set[String]] = {
    val key = buildRoute(route)
    subscriptions.flatMap(_.merge(key, Set(id))(_ union _)).commit
  }

  private val writeTopic: Chunk[String] => IO[Nothing, Unit] =
    (keys: Chunk[String]) =>
      STM.atomically {
        for {
          map <- subscriptions
          _   <- STM.foreach_(keys)(key => STM.succeed(map.merge(key, Set.empty[String])(_ union _)))
        } yield ()
      }

  private val readSubscribers: Chunk[String] => UIO[Set[String]] =
    (keys: Chunk[String]) =>
      STM.atomically {
        for {
          subs <- subscriptions
          sets <- STM.foreach(keys)(key => subs.getOrElse(key, Set.empty[String]))
        } yield sets.fold(Set.empty[String])(_ union _)
      }

  private val readSubscribersAndWriteTopics: Chunk[String] => UIO[Set[String]] =
    (keys: Chunk[String]) =>
      STM.atomically {
        for {
          subs <- subscriptions
          sets <- STM.foreach(keys)(key => subs.merge(key, Set[String]())(_ union _))
        } yield sets.fold(Set.empty[String])(_ union _)
      }

  private def getSubRoutes(route: NonEmptyChunk[String]): Chunk[String] =
    route.scan("")((acc, c) => acc + c).drop(1)

  private def buildRoute(route: NonEmptyChunk[String]): String =
    route.mkString
}