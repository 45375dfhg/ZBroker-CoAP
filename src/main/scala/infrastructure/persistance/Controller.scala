package infrastructure.persistance

import domain.model.config.ConfigRepository
import infrastructure.environment._
import subgrpc.subscription.PublisherResponse
import zio._
import zio.console.putStrLn

object Controller {

  def boot(args: List[String]) = {
    // val profile = args.headOption.fold("")(identity)

    putStrLn("[Config] Applying config from memory. Pass 'console' as an argument to set config manually.").toLayer ++
      getEnvironment(ConfigRepositoryEnvironment.fromMemory)
  }

  /**
   * Constructs an environment where the only missing part of the layer is a ConfigRepository.Service.
   * Said layer is provided as parameter.
   */
  def getEnvironment(config: ULayer[Has[ConfigRepository.Service]]) =
    BrokerRepositoryEnvironment.fromSTM[PublisherResponse] ++ (ZEnv.live >+> config >+> EndpointEnvironment.fromChannel) >+>
      (ChunkStreamRepositoryEnvironment.fromSocket ++ MessageSenderRepositoryEnvironment.fromSocket)

}

//    if (profile != "console") putStrLn("[Config] Applying config from memory. Pass 'console' as an argument to set manually").toLayer ++ getEnvironment(ConfigRepositoryEnvironment.fromMemory)
//    else putStrLn("[Config] Applying config from console input ...").toLayer ++ getEnvironment(ConfigRepositoryEnvironment.fromConsole)