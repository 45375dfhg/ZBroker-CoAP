package domain.model.coap

/**
 * The Media Format of an 
 */
sealed trait CoapPayloadMediaTypes
case object TextMediaType        extends CoapPayloadMediaTypes
case object LinkMediaType        extends CoapPayloadMediaTypes
case object XMLMediaType         extends CoapPayloadMediaTypes
case object OctetStreamMediaType extends CoapPayloadMediaTypes
case object EXIMediaType         extends CoapPayloadMediaTypes
case object JSONMediaType        extends CoapPayloadMediaTypes

// DEFAULT FALLBACK
case object SniffingMediaType    extends CoapPayloadMediaTypes