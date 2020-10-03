
import infrastructure.environment._
import infrastructure.persistance._
import zio._


object Main extends App {

  val program = PublisherServer.make <&> SubscriberServer.make

  val env =
    BrokerRepositoryEnvironment.fromSTM ++
      (ConfigRepositoryEnvironment.fromMemory >+> EndpointEnvironment.fromChannel) >+>
      (ChunkStreamRepositoryEnvironment.fromSocket ++ MessageSenderRepositoryEnvironment.fromSocket)

  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    program.provideCustomLayer(env).orDie.exitCode

}

