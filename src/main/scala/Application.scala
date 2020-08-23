import infrastructure.stream.ChunkStreamFromSocket
import domain.model.stream.ChunkStreamRepository
import zio.App
import zio.console._

import zio.stream._

object Application extends App {

  val program =
    (for {
      _         <- ZStream.fromEffect(putStrLn("booting up ..."))
      _         <- ChunkStreamRepository.getStream
    } yield ()).runDrain

  val partialLayer = ChunkStreamFromSocket.live

  def run(args: List[String]) =
    program.tap(l => putStrLn(l.toString)).provideCustomLayer(partialLayer).orDie.exitCode

}

