package domain.model.coap

sealed trait CoapPayloadContentFormat
case object TextFormat        extends CoapPayloadContentFormat
case object LinkFormat        extends CoapPayloadContentFormat
case object XMLFormat         extends CoapPayloadContentFormat
case object OctetStreamFormat extends CoapPayloadContentFormat
case object EXIFormat         extends CoapPayloadContentFormat
case object JSONFormat        extends CoapPayloadContentFormat

// DEFAULT FALLBACK
case object SniffingFormat    extends CoapPayloadContentFormat