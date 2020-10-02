
import infrastructure.environment._

import zio._


object Main extends App {

  val program = Program.coapStream <&> SubscriptionServer.logic

  val partialLayer =
    BrokerRepositoryEnvironment.fromSTM ++ (ConfigRepositoryEnvironment.fromMemory >+> EndpointEnvironment.fromChannel) >+>
      (ChunkStreamRepositoryEnvironment.fromSocket ++
        MessageSenderRepositoryEnvironment.fromSocket)

  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    program.provideCustomLayer(partialLayer).orDie.exitCode

}

