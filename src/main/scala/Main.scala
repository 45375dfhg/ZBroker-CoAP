
import infrastructure.environment._

import zio._
import zio.console._


object Main extends App {

  val program = Program.coapStream <&> SubscriptionServer.logic

  val partialLayer =
    BrokerRepositoryEnvironment.fromSTM ++ (ConfigRepositoryEnvironment.fromMemory >+> EndpointEnvironment.fromChannel) >+>
      (ChunkStreamRepositoryEnvironment.fromSocket ++
        MessageSenderRepositoryEnvironment.fromSocket)

  def run(args: List[String]): URIO[ZEnv with Console, ExitCode] =
    program.provideCustomLayer(partialLayer).orDie.exitCode

}

