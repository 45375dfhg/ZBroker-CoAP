import zio._
import zio.stm._


class DuplicationTracker[A](val fleeting: TSet[A]) {

  def add(element: A): UIO[Boolean] =
    STM.atomically {
      for {
        b <- fleeting.contains(element)
        _ <- STM.unless(b)(fleeting.put(element))
      } yield b
    }

  def remove(element: A): UIO[Unit] =
    fleeting.delete(element).commit

}

object DuplicationTracker {

  def make[A]: USTM[DuplicationTracker[A]] =
    for {
      fleeting <- TSet.empty[A]
      tracker  =  new DuplicationTracker[A](fleeting)
    } yield tracker
}