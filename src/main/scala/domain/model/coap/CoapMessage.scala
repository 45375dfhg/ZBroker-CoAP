package domain.model.coap

import domain.model.RouteModel.Route
import domain.model.coap.header.CoapId
import domain.model.coap.option.StringCoapOptionValueContent
import domain.model.exception._
import subgrpc.subscription.{Path, PublisherResponse}
import zio.{IO, NonEmptyChunk}

final case class CoapMessage(header: CoapHeader, body: CoapBody) {

  def isConfirmable: Boolean    = this.header.msgType.value == 0
  def isNonConfirmable: Boolean = this.header.msgType.value == 1

  val getRoute: IO[SuccessfulFailure, NonEmptyChunk[String]] = {
    this.body.options match {
      case Some(optionChunk) =>
        val routes = optionChunk.collect {
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
    (getRoute <*> getContent).map { case (route, content) => PublisherResponse(Some(Path(route.toSeq)), content) }


}

object CoapMessage {
  def reset(id : CoapId) = CoapMessage(CoapHeader.reset(id), CoapBody.empty)
  def ack  (id : CoapId) = CoapMessage(CoapHeader.ack(id), CoapBody.empty)
}







