package infrastructure.persistance.dupetracker

import domain.model.dupetracker._

import zio._
import zio.stm._


class DuplicationTracker[A](val fleeting: TSet[A]) extends DuplicationTrackerRepository.Service[A] {

  /**
   * Adds an element to the set if it is not already included.
   * @param element An element of A which is to be added to the fleeting TSet[A].
   * @return A Boolean that represents whether the element was added to the TSet. true means it was added.
   */
  def add(element: A): UIO[Boolean] =
    STM.atomically {
      for {
        b <- fleeting.contains(element)
        _ <- STM.unless(b)(fleeting.put(element))
      } yield !b
    }

  /**
   * Removes an element from the set if it is included.
   * @param element An element of A which is to be added to the fleeting TSet[A].
   * @return A boolean that represents whether the element was removed from the set. true means it was removed.
   */
  def remove(element: A): UIO[Boolean] =
    STM.atomically {
      for {
        b <- fleeting.contains(element)
        _ <- STM.when(b)(fleeting.delete(element))
      } yield b
    }

}

object DuplicationTracker {

  def make[A]: USTM[DuplicationTracker[A]] =
    for {
      fleeting <- TSet.empty[A]
      tracker  =  new DuplicationTracker[A](fleeting)
    } yield tracker
}