package domain.model.coap.body

import fields._

//final case class CoapOption(
//  delta    : CoapOptionDelta,
//  exDelta  : Option[CoapOptionExtendedDelta],
//  length   : CoapOptionLength,
//  exLength : Option[CoapOptionExtendedLength],
//  number   : CoapOptionNumber,
//  optValue : CoapOptionValue,
//  offset   : CoapOptionOffset
//)

final case class CoapOption(
  coapOptionDelta  : CoapOptionDelta,
  coapOptionLength : CoapOptionLength,
  coapOptionNumber : CoapOptionNumber,
  coapOptionValue  : CoapOptionValue,
)
