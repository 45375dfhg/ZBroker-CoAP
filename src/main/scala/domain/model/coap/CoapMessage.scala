package domain.model.coap

import domain.model.coap.header.CoapId
import domain.model.coap.option.StringCoapOptionValueContent
import domain.model.exception.{MissingOptions, MissingRoutes, SuccessfulFailure}

import zio.NonEmptyChunk

final case class CoapMessage(header: CoapHeader, body: CoapBody) {

  def isConfirmable: Boolean    = this.header.msgType.value == 0
  def isNonConfirmable: Boolean = this.header.msgType.value == 1

  def getRoutes: Either[SuccessfulFailure, NonEmptyChunk[String]] = {
    this.body.options match {
      case Some(optionChunk) =>
        val routes = optionChunk.collect {
          case option if option.number.value == 11 => option.optValue.content
        } collect { case StringCoapOptionValueContent(value) => value }
        NonEmptyChunk.fromChunk(routes).toRight(MissingRoutes)
      case None => Left(MissingOptions)
    }
  }

}

object CoapMessage {
  def reset(id : CoapId) = CoapMessage(CoapHeader.reset(id), CoapBody.empty)
  def ack  (id : CoapId) = CoapMessage(CoapHeader.ack(id), CoapBody.empty)
}







