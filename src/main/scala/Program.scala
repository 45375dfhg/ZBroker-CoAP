
import Controller._
import domain.api.CoapDeserializerService.{IgnoredMessageWithId, IgnoredMessageWithIdOption}
import domain.api._
import domain.model.chunkstream.ChunkStreamRepository
import domain.model.coap._
import domain.model.exception.{MissingAddress, MissingCoapId, UnexpectedError}
import domain.model.sender.MessageSenderRepository
import zio.{IO, UIO, ZManaged}
import zio.console._
import zio.nio.core.SocketAddress
import zio.stream.ZStream

object Controller {
  val boot = putStrLn("The application is starting. Settings need to be configured.")
}

object Program {

  // TODO: CHANGE ALL OF THIS LOL
  val start = ZStream.fromEffect(boot)

  // TODO: REFACTOR
  val coapStream =
    ChunkStreamRepository
      .getChunkStream
      .mapM { case (address, chunk) => UIO(address) <*> CoapDeserializerService.extractFromChunk(chunk) } // TODO: REFACTOR THE EITHER PART
      .collect(errorContainsID)
      .tap { case (address, either) => either match {
        case Left(value) => {
          val msg = ResponseService.getResetMessage(value)
          IO.fromOption(address).orElseFail(MissingAddress).flatMap(MessageSenderRepository.sendMessage(_, msg)).either
        }
        case Right(_) => UIO.succeed("Nothing to do.")
      }}
      .collect { case (address, either) => either match {
        case Right(value) => (address, value)
      }}



//      .broadcast(2, 16)
//      .flatMap { case a :: b :: Nil =>
//        val lefts  = a.collect { case (address, either) => either match { case Left(value) => (address, value) } }
//        val rights = b.map(_._2)
//      }



//      .groupByKey(_._2.isRight) {
//        case (true, stream) => stream.mapM({ case (i, o) => IO(i) <*> IO.fromEither(o). })
//        case (false, stream) => stream.mapM(
//          { case (i, c) =>
//            (IO.fromOption(i).orElseFail(MissingAddress) <*>
//              IO.fromEither(c.swap).bimap(_ => UnexpectedError("critical"), id => ResponseService.getResetMessage(id)))
//              .flatMap(t => MessageSenderRepository.sendMessage(t._1, t._2)).
//          })
//        }.filter(a => )
//      .flatMap()


// (IO.fromOption(i).orElseFail(MissingAddress) <*>
//      .tap(o => putStrLn(o._2.toString))
//      .tap( { case (i, c) =>
//        (IO.fromOption(i).orElseFail(MissingAddress) <*>
//          ResponseService.getResponse(c) >>=
//          (t => MessageSenderRepository.sendMessage(t._1, t._2))).either
//      })
//      .tap(o => putStrLn(o._2.toString))

  // .partition(o => ResponseService.hasResponse(o._2), 2048)

  /* 1. (address, chunk) =>
     2. if (failed) sendMessage else {
         check code code and match the requested action (basically only allow get and put for now)
        } then send response if necessary
     3. if correct code was sent (put)


   */

  private def errorContainsID: PartialRemoveEmptyID = {
    case (address, Right(value)) => (address, Right(value))
    case (address, Left((err, id))) if id.isDefined => (address, Left(err, id.get))
  }


//  private def errorContainsID: PartialRemoveEmptyID = {
//    case (address, either) => either match {
//      case Right(m)                        => (address, Right(m))
//      case Left((err, id)) if id.isDefined => (address, Left(err, id.get))
//      case _ =>
//    }}

//  private def containsId: PartialFunction[Either[IgnoredMessageWithIdOption, CoapMessage], Either[IgnoredMessageWithId, CoapMessage]] = {
//    case Right(m)                          => Right(m)
//    case Left((err, opt)) if opt.isDefined => Left(err, opt.get) // this is not very idiomatic since it's using .get!
//  }

  type PartialRemoveEmptyID = PartialFunction[(Option[SocketAddress], Either[IgnoredMessageWithIdOption, CoapMessage]),
    (Option[SocketAddress], Either[IgnoredMessageWithId, CoapMessage])]

  private val ignore = MissingCoapId // wrong - should be an actual unexpected kind of exception
}

/**
val response = ResponseService.getResponse(c)
        val address  = i.toRight(MissingAddress)
        val cool = for {
          r <- response
          a <- address
        } yield (a, r)
        IO.fromEither(cool).flatMap({ case (fuck, me) => MessageSenderRepository.sendMessage(fuck, me)}).either

*/