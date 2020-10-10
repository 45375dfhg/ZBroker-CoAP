package infrastructure.environment

import domain.model.broker.BrokerRepository._
import infrastructure.persistance.broker._
import zio._


object BrokerRepositoryEnvironment {

  def fromSTM[R: Tag]: ULayer[BrokerRepository[R]] = TransactionalBroker.make[R].commit.toLayer
}