package infrastructure.environment

import domain.model.broker.BrokerRepository._
import infrastructure.persistance.broker._
import zio._


object BrokerRepositoryEnvironment {

  val fromSTM : ULayer[BrokerRepository] = TransactionalBroker.make.commit.toLayer
}