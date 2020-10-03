package infrastructure.environment

import domain.model.config._

import infrastructure.persistance.config._
import zio._

object ConfigRepositoryEnvironment {

  val fromMemory: ULayer[Has[ConfigRepository.Service]] = ZLayer.succeed(ConfigRepositoryInMemory)

  val fromConsole: ULayer[Has[ConfigRepository.Service]] = ZLayer.succeed(ConfigRepositoryFromConsole)
}