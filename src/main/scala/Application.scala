import domain.model.stream.ChunkStreamRepository

import infrastructure.config.ConfigRepositoryInMemory
import infrastructure.endpoint.EndpointRepositoryFromSocket
import infrastructure.stream.{ChunkStreamFromSocket, OutgoingStream}

import zio.App
import zio.console._
import zio.stream._

object Application extends App {

  val program =
    (for {
      _ <- ZStream.fromEffect(putStrLn("booting up ..."))
      _ <- ZStream.mergeAll(2, 16)(ChunkStreamRepository.getStream, OutgoingStream.send)
    } yield ()).runDrain

  val partialLayer = (
    ConfigRepositoryInMemory.live
    >+> EndpointRepositoryFromSocket.live
    >+> ChunkStreamFromSocket.live)

  def run(args: List[String]) =
    program.tap(l => putStrLn(l.toString)).provideCustomLayer(partialLayer).orDie.exitCode

}

