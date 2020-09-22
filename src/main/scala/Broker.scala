
import domain.model.coap.option.CoapOptionValueContent
import zio.{Chunk, NonEmptyChunk, UIO, ZIO}
import zio.stm._

/**
 * There need to be two structures:
 * 1. Every route has zero or more subscribers
 * 2. Every route is mapped to a device's IP
 * -> separated to avoid overhead due to copying
 *
 * A subscriber IS its own address / the related connection
 *
 * Note: Subscriber should NOT know about IPs
 * Question: What if two devices provide information to the same route? List?
 */
object Broker {

  private val subscriptions = TMap.empty[String, Set[String]]

  def addTopic(routes: NonEmptyChunk[String]): UIO[Unit] = {
    (for {
      map <- subscriptions
      arr <- TArray.fromIterable(routes.scan("")((acc, c) => acc + c).drop(1).map(_ -> Set[String]()))
      _   <- arr.foreach(t => STM.succeed(map.merge(t._1, t._2)(_ union _)))
    } yield ()).commit
  }

  /**
   *
   * @param route the route where to add a new subscriber
   * @param id
   * @return the set of subscribers on that specific route
   */
  def addSubscriber(route: String, id: String): UIO[Set[String]] =
    subscriptions.flatMap(_.merge(route, Set(id))(_ union _)).commit

  def getSubscribers(routes: NonEmptyChunk[String]): UIO[Set[String]] = {
    val keys = routes.scan("")((acc, c) => acc + c).drop(1)
    STM.atomically {
      for {
        subs <- subscriptions
        sets <- STM.foreach(keys)(key => subs.getOrElse(key, Set.empty[String]))
      } yield sets.fold(Set.empty[String])(_ union _)
    }
  }

  def onIncomingMessage(routes: NonEmptyChunk[String]): UIO[Set[String]] = {
    val keys = routes.scanLeft("")((acc, c) => acc + c).drop(1)
    STM.atomically {
      for {
        subs <- subscriptions
        sets <- STM.foreach(keys)(key => subs.merge(key, Set[String]())(_ union _))
      } yield sets.fold(Set.empty[String])(_ union _)
    }
  }

}


//  sealed trait Trie[+A, +B]
//  sealed trait Empty extends Trie[Nothing, Nothing]
//  case object Empty extends Empty
//
//  final case class Node[A, B](subscriber: Set[B], children: Map[A, Trie[A, B]]) extends Trie[A, B]
//
//  val subscriptions = TMap.empty[String, (Set[String], Node[String, Int])] // just an example
//
//  def addPublisher() = ???
//
//  def addSubscriber() = ???
//
//  def publish(routes: NonEmptyChunk[String]) = {
//
//    def loop(rem: Chunk[String]) =
//      rem.headOption match {
//        case Some(key) => (key -> Node(Set[String](), Map(loop))
//        case None       => Empty
//      }
//
//    for {
//      subs <- subscriptions
//      _    <- if subs.contains(routes.head)
//    } yield ()
//  }


//val a: Node[String, String] = Node(Set("a", "b"), HashMap.empty)
//val b: Node[String, String] = Node(Set("c"), HashMap("room1" -> a))
//
//val c = (for {
//  set <- TMap.make("home1" -> b)
//} yield set).commit
//
//
//
//
//def fn(map: TMap[String, Node[String, String]]) =
//  (for {
//    a <- map.merge("home1", Node(Set("d"), HashMap.empty))((one, two) => one.copy(subscriber = one.subscriber union two.subscriber))
//  } yield a).commit
//
//
//val d = c.tap(fn).flatMap(_.get("home1").commit).flatMap({
//  case Some(node) => ZIO.succeed(node.subscriber)
//  case None       => ZIO.fail("")
//}).tap(a => ZIO.foreach(a)(putStrLn))