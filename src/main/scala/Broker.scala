
import zio.ZIO
import zio.console.putStrLn
import zio.stm._

import scala.collection.immutable.HashMap

final case class Node[A, B](subscriber: Set[B], children: Map[A, Trie[A, B]]) extends Trie[A, B]

sealed trait Trie[+A, +B]
sealed trait Empty extends Trie[Nothing, Nothing]
case object Empty extends Empty

val a: Node[String, String] = Node(Set("a", "b"), HashMap.empty)
val b: Node[String, String] = Node(Set("c"), HashMap("room1" -> a))

val c = (for {
  set <- TMap.make("home1" -> b)
} yield set).commit

def fn(map: TMap[String, Node[String, String]]) =
  (for {
    a <- map.merge("home1", Node(Set("d"), HashMap.empty))((one, two) => one.copy(subscriber = one.subscriber union two.subscriber))
  } yield a).commit


val d = c.tap(fn).flatMap(_.get("home1").commit).flatMap({
  case Some(node) => ZIO.succeed(node.subscriber)
  case None       => ZIO.fail("")
}).tap(a => ZIO.foreach(a)(putStrLn))