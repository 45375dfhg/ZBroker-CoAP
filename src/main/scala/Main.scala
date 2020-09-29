
import infrastructure.environment._
import zio._
import zio.console._


object Main extends App {

  val logic = Program.coapStream

  val partialLayer =
    (ConfigRepositoryEnvironment.fromMemory >+> EndpointEnvironment.fromChannel) >+>
      (ChunkStreamRepositoryEnvironment.fromSocket ++
        MessageSenderRepositoryEnvironment.fromSocket ++
        BrokerRepositoryEnvironment.fromSTM)

  def run(args: List[String]): URIO[ZEnv with Console, ExitCode] =
    logic.provideCustomLayer(partialLayer).orDie.exitCode

}

