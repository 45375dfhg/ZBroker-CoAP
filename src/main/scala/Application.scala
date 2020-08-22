import domain.model.endpoint.EndpointRepository
import domain.model.stream.StreamRepository
import infrastructure.config.ConfigRepositoryInMemory
import infrastructure.endpoint.EndpointRepositoryFromSocket
import infrastructure.stream.StreamRepositoryFromSocket

import zio._
import zio.App
import zio.console._
import zio.stream._

object Application extends App {

  val program =
    (for {
      channel <- ZStream.managed(EndpointRepository.getDatagramEndpoint)
      _       <- ZStream.mergeAll(2, 16)(StreamRepository.getDatagramStream(channel), StreamRepository.sendDatagramStream(channel))
    } yield ()).runDrain

  val partialLayer =
    ConfigRepositoryInMemory.live >+> (EndpointRepositoryFromSocket.live ++ StreamRepositoryFromSocket.live)

  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    program.tap(l => putStrLn(l.toString)).provideCustomLayer(partialLayer).orDie.exitCode

}

