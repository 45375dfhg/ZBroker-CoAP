
import domain.api.CoapDeserializerService
import domain.model.chunkstream.ChunkStreamRepository
import infrastructure.environment.{ChunkStreamRepositoryEnvironment, ConfigRepositoryEnvironment, EndpointEnvironment, MessageSenderRepositoryEnvironment}
import infrastructure.persistance.chunkstream.OutgoingStream
import zio.App
import zio.console._
import zio.stream._

object Main extends App {

//  val program =
//    (for {
//      _ <- ZStream.fromEffect(putStrLn("booting up ..."))
//      _ <- ZStream.mergeAll(2, 16)(
//        ChunkStreamRepository
//          .getChunkStream
//          .tap(b => putStrLn(b._2.toString))
//          .mapM({ case (_, c) => CoapDeserializerService.extractFromChunk(c) })
//          // refactor the inline collect into its own function
//          .tap(a => putStrLn(a.toString))
//        , OutgoingStream.send)
//    } yield ()).runDrain

  val prog = Program.coapStream.runDrain

  val partialLayer = (
    ConfigRepositoryEnvironment.fromMemory
    >+> EndpointEnvironment.fromChannel
    >+> (ChunkStreamRepositoryEnvironment.fromSocket ++ MessageSenderRepositoryEnvironment.fromSocket))

  def run(args: List[String]) =
    prog.tap(l => putStrLn(l.toString)).provideCustomLayer(partialLayer).orDie.exitCode

}

