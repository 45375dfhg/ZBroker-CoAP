package infrastructure.environment

import domain.model.broker.BrokerRepository.BrokerRepository
import infrastructure.persistance.broker.TransactionalBroker
import zio.{ULayer, ZLayer}

object BrokerRepositoryEnvironment {

  val fromSTM : ULayer[BrokerRepository]= ZLayer.succeed(TransactionalBroker)
}