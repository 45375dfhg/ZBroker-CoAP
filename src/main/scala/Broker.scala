
import zio.{Has, ZIO}
import zio.console.putStrLn
import zio.stm._

import scala.collection.immutable.HashMap

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

  sealed trait Trie[+A, +B]
  sealed trait Empty extends Trie[Nothing, Nothing]
  case object Empty extends Empty

  final case class Node[A, B](subscriber: Set[B], children: Map[A, Trie[A, B]]) extends Trie[A, B]

  val subscriptions = TMap.empty[String, Node[String, Int]] // replace string with a ROUTE type



}



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