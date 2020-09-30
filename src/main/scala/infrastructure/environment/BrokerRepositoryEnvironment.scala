package infrastructure.environment

import domain.model.broker.BrokerRepository.BrokerRepository
import infrastructure.persistance.broker.TransactionalBroker
import zio.ULayer


object BrokerRepositoryEnvironment {

  val fromSTM : ULayer[BrokerRepository] = TransactionalBroker.make.commit.toLayer
}