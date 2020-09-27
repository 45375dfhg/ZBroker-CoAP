package infrastructure.environment

import domain.model.broker.BrokerRepository.BrokerRepository
import infrastructure.persistance.broker.BrokerSTM
import zio.{ULayer, ZLayer}

object BrokerRepositoryEnvironment {

  val fromSTM : ULayer[BrokerRepository]= ZLayer.succeed(BrokerSTM)
}