package infrastructure.environment

import domain.model.dupetracker.DuplicationTrackerRepository._
import infrastructure.persistance.dupetracker._
import zio._

object DuplicationTrackerRepositoryEnvironment {

  def fromSTM[A]: ULayer[DuplicationTrackerRepository[A]] = DuplicationTracker.make[A].commit.toLayer
}