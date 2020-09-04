import domain.model.stream.ChunkStreamRepository
import infrastructure.config.ConfigRepositoryInMemory
import infrastructure.endpoint.EndpointRepositoryFromSocket
import infrastructure.stream.{ChunkStreamFromSocket, OutgoingStream}
import root._
import zio.{App, ZIO}
import zio.console._
import zio.stream._

object Application extends App {

  val program =
    (for {
      _ <- ZStream.fromEffect(putStrLn("booting up ..."))
      _ <- ZStream.mergeAll(2, 16)(
        ChunkStreamRepository
          .getStream
          .tap(b => putStrLn(b._2.toString))
          .mapM(e => ZIO.fromEither(CoapService.extractFromChunk(e._2)))
          .tap(a => putStrLn(a.toString)),
        OutgoingStream.send)
    } yield ()).runDrain

  val partialLayer = (
    ConfigRepositoryInMemory.live
    >+> EndpointRepositoryFromSocket.live
    >+> ChunkStreamFromSocket.live)

  def run(args: List[String]) =
    program.tap(l => putStrLn(l.toString)).provideCustomLayer(partialLayer).orDie.exitCode

}

