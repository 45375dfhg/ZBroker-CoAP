package domain.model.coap

import domain.model.coap.body._
import domain.model.coap.body.fields._
import domain.model.coap.header._
import domain.model.coap.header.fields._
import domain.model.exception._
import subgrpc.subscription._
import zio._

final case class CoapMessage(header: CoapHeader, body: CoapBody) { self =>

  def toByteChunk: Chunk[Byte] =
    header.toByteChunk ++ body.toByteChunk

  def isConfirmable: Boolean    = this.header.coapType.value == 0
  def isNonConfirmable: Boolean = this.header.coapType.value == 1

  val getPath: IO[SuccessfulFailure, NonEmptyChunk[String]] = {
    this.body.options match {
      case Some(optionChunk) =>
        val routes = optionChunk.value
           .collect {
            case option if option.coapOptionNumber.value == 11 => option.coapOptionValue.content
          }.collect {
           case StringCoapOptionValueContent(value) => value
        }
        IO.fromOption(NonEmptyChunk.fromChunk(routes)).orElseFail(MissingRoutes)
      case None => IO.fail(MissingOptions)
    }
  }

  val hasPath: Boolean =
    this.body.options match {
      case Some(options) => options.value.find(_.coapOptionNumber.value == 11).isDefined
      case _             => false
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

  // Kinda poor validation sequencing but enough for now
  def validated = IO.cond(valid, self, InvalidEmptyMessage)

  private val isReset        = header.coapType.value == 3
  private val hasEmptyCode   = header.coapCodePrefix.value == 0 && self.header.coapCodeSuffix.value == 0
  private val tokenZero      = header.coapTokenLength.value == 0
  private val isTokenLess    = body.token.isEmpty
  private val isOptionLess   = body.options.isEmpty
  private val isPayloadLess  = body.options.isEmpty

  private val isEmptyMessage = hasEmptyCode && tokenZero && isTokenLess && isOptionLess && isPayloadLess
  private val valid          = (!isReset && !hasEmptyCode) || (hasEmptyCode && isEmptyMessage)

  val toPublisherResponse: IO[SuccessfulFailure, PublisherResponse] =
    (getPath <*> getContent).map { case (route, content) => toPublisherResponseWith(route, content) }

  private def toPublisherResponseWith(route: NonEmptyChunk[String], content: String) =
    PublisherResponse(Some(Path(route.toSeq)), content)

}

object CoapMessage {

  /**
   * DEPRECATED
   */
  def asResetWith(id : CoapId) =
    CoapMessage(CoapHeader.reset(id), CoapBody.empty)

  /**
   * DEPRECATED
   */
  def asAckWith(id : CoapId) =
    CoapMessage(CoapHeader.ack(id), CoapBody.empty)

  def asAckFrom(msg: CoapMessage) =
    CoapMessage(CoapHeader.ack(msg.header.coapId), CoapBody.asEmptyResponse(msg.body.token))

  def asResetFrom(msg: CoapMessage) =
    CoapMessage(CoapHeader.reset(msg.header.coapId), CoapBody.asEmptyResponse(msg.body.token))

  def fromDatagram(datagram: Chunk[Byte]): IO[GatewayError, CoapMessage] =
    (for {
      header    <- CoapHeader.fromDatagram(datagram)
      body      <- CoapBody.fromDatagramWith(datagram, header)
    } yield CoapMessage(header, body)).flatMap(_.validated)

}







