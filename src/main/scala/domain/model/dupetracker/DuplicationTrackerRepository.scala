package domain.model.dupetracker

import zio._

object DuplicationTrackerRepository {

  type DuplicationTrackerRepository[A] = Has[DuplicationTrackerRepository.Service[A]]

  trait Service[A] {
    def addIf(element: A): UIO[Boolean]
    def remove(element: A): UIO[Boolean]
    def size: UIO[Int]
  }

  def add[A: Tag](element: A): URIO[DuplicationTrackerRepository[A], Boolean] =
    ZIO.accessM(_.get.addIf(element))

  def remove[A: Tag](element: A): URIO[DuplicationTrackerRepository[A], Boolean] =
    ZIO.accessM(_.get.remove(element))

  def size[A: Tag]: URIO[DuplicationTrackerRepository[A], Int] =
    ZIO.accessM(_.get.size)
}