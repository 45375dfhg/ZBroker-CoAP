
import Controller._
import domain.api.CoapDeserializerService.{IgnoredMessageWithId, IgnoredMessageWithIdOption}
import domain.api._
import domain.model.chunkstream.ChunkStreamRepository
import domain.model.coap._
import domain.model.exception.{MissingAddress, MissingCoapId}
import domain.model.sender.MessageSenderRepository
import zio.{IO, UIO}
import zio.console._
import zio.stream.ZStream

object Controller {
  val boot = putStrLn("The application is starting. Settings need to be configured.")
}

object Program {

  // TODO: CHANGE ALL OF THIS LOL
  val start = ZStream.fromEffect(boot)

  val coapStream =
    ChunkStreamRepository
      .getChunkStream
      .mapM({ case (i, c) => UIO(i) <*> CoapDeserializerService.extractFromChunk(c).collect(ignore)(containsId) })
      .tap(o => putStrLn(o._2.toString))
      .tap( { case (i, c) =>
        UIO.succeed(i) <*> ResponseService.getResponse(c) >>= (t => MessageSenderRepository.sendMessage(t._1, t._2))
      })
      .tap(o => putStrLn(o._2.toString))

  // .partition(o => ResponseService.hasResponse(o._2), 2048)

  // derive reply *> if reply send it *> in next tap push to STM no matter what

  private def containsId: PartialFunction[Either[IgnoredMessageWithIdOption, CoapMessage], Either[IgnoredMessageWithId, CoapMessage]] = {
    case Right(m)                          => Right(m)
    case Left((err, opt)) if opt.isDefined => Left(err, opt.get) // this is not very idiomatic since it's using .get!
  }

  private val ignore = MissingCoapId // wrong - should be an actual unexpected kind of exception
}