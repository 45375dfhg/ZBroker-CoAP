package domain.model.dupetracker

import zio._

object DuplicationTrackerRepository {

  type DuplicationTrackerRepository[A] = Has[DuplicationTrackerRepository.Service[A]]

  trait Service[A] {
    def add(element: A): UIO[Boolean]
    def remove(element: A): UIO[Unit]
  }

  def add[A](element: A): URIO[DuplicationTrackerRepository[A], Boolean] =
    ZIO.accessM(_.get.add(element))

  def remove[A](element: A): URIO[DuplicationTrackerRepository[A], Unit] =
    ZIO.accessM(_.get.remove(element))
}