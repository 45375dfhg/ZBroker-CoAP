package domain.model.coap

import domain.model._

final case class COAPMessage(header: ChunkByteWrapper, body: ChunkByteWrapper)