package infrastructure.environment

import domain.model.config.ConfigRepository
import infrastructure.persistance.config.ConfigRepositoryInMemory
import zio.{Has, Layer, ZLayer}

object ConfigRepositoryEnvironment {

  val fromMemory: Layer[Nothing, Has[ConfigRepository.Service]] = ZLayer.succeed(ConfigRepositoryInMemory)
}