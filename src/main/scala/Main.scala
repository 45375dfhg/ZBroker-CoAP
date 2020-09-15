
import infrastructure.environment._

import zio.App
import zio.console._


object Main extends App {

  val prog = Program.coapStream.runDrain

  val partialLayer = (
    ConfigRepositoryEnvironment.fromMemory
    >+> EndpointEnvironment.fromChannel
    >+> (ChunkStreamRepositoryEnvironment.fromSocket ++ MessageSenderRepositoryEnvironment.fromSocket))

  def run(args: List[String]) =
    prog.tap(l => putStrLn(l.toString)).provideCustomLayer(partialLayer).orDie.exitCode

}

