package domain.api

import domain.model.dupetracker.DuplicationTrackerRepository
import domain.model.dupetracker.DuplicationTrackerRepository._

import zio.clock._
import zio.duration._
import zio._

object DuplicateRejectionService {

  /**
   * Adds an element of A to the underlying collection and removes it afterwards.
   *
   * @param element An element of A that is to be added fleetingly to the underlying collection.
   * @param n       The number of seconds of delay to the removal.
   * @return A Boolean value which represents whether an element was added (and removed).
   *         true means it was added and WILL be removed (after n-seconds).
   */
  def temporaryAdd[A: Tag](element: A, n: Int = 145): URIO[DuplicationTrackerRepository[A] with Clock, Boolean] =
    for {
      b <- DuplicationTrackerRepository.add(element)
      _ <- ZIO.when(b)(DuplicationTrackerRepository.remove(element).delay(n.seconds).fork)
    } yield b

  def size[A: Tag]: URIO[DuplicationTrackerRepository[A], Int] =
    DuplicationTrackerRepository.size
}
