package infrastructure.persistance

import domain.model.config.ConfigRepository
import infrastructure.environment._
import zio._
import zio.console.putStrLn

object Controller {

  def boot(args: List[String]) = {
    val profile = args.headOption.fold("")(identity)

    if (profile == "memory") putStrLn("what").toLayer ++ getEnvironment(ConfigRepositoryEnvironment.fromMemory)
    else putStrLn("what").toLayer ++ getEnvironment(ConfigRepositoryEnvironment.fromConsole)
  }

  /**
   * Constructs an environment where the only missing part of the layer is a ConfigRepository.Service.
   * Said layer is provided as parameter.
   */
  def getEnvironment(config: ULayer[Has[ConfigRepository.Service]]) =
    BrokerRepositoryEnvironment.fromSTM ++
      (config >+> EndpointEnvironment.fromChannel) >+>
      (ChunkStreamRepositoryEnvironment.fromSocket ++ MessageSenderRepositoryEnvironment.fromSocket)

}