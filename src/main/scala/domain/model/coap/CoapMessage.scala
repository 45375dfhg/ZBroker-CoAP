package domain.model.coap

import domain.api.CoapDeserializerService
import domain.model.coap.body._
import domain.model.coap.body.fields._
import domain.model.coap.header._
import domain.model.exception._
import subgrpc.subscription._
import zio._

final case class CoapMessage(header: CoapHeader, body: CoapBody) {

  def isConfirmable: Boolean    = this.header.coapType.value == 0
  def isNonConfirmable: Boolean = this.header.coapType.value == 1

  // TODO: COAPOPTION VALUE NEEDS REWRITE!
  val getPath: IO[SuccessfulFailure, NonEmptyChunk[String]] = {
    this.body.options match {
      case Some(optionChunk) =>
        val routes = optionChunk.value
           .collect {
            case option if option.number.value == 11 => option.optValue.content
          }.collect {
           case StringCoapOptionValueContent(value) => value
        }
        IO.fromOption(NonEmptyChunk.fromChunk(routes)).orElseFail(MissingRoutes)
      case None => IO.fail(MissingOptions)
    }
  }

  val getContent: IO[SuccessfulFailure, String] = {
    this.body.payload match {
      case Some(payload) => payload match {
        case TextCoapPayload(value) => IO.succeed(value)
        case _                      => IO.fail(UnsupportedPayload)
      }
      case None          => IO.fail(MissingPayload)
    }
  }

  val toPublisherResponse: IO[SuccessfulFailure, PublisherResponse] =
    (getPath <*> getContent).map { case (route, content) => toPublisherResponseWith(route, content) }

  private def toPublisherResponseWith(route: NonEmptyChunk[String], content: String) =
    PublisherResponse(Some(Path(route.toSeq)), content)

}

object CoapMessage {
  def reset(id : CoapId) = CoapMessage(CoapHeader.reset(id), CoapBody.empty)
  def ack  (id : CoapId) = CoapMessage(CoapHeader.ack(id), CoapBody.empty)

  def fromDatagram(datagram: Chunk[Byte]) =
    for {
      header  <- CoapHeader.fromDatagram(datagram)
      body    <- chunk.dropExactly(4) >>= (bodyFromChunk(_, header))
      message <- validateMessage(header, body)
    } yield message

  def fromChunk(chunk: Chunk[Byte]): UIO[Either[(MessageFormatError, Option[CoapId]), CoapMessage]] =
    CoapDeserializerService.extractFromChunk(chunk)
}







