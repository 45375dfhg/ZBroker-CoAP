package domain.model.interface

import domain.model._

sealed trait Interface
final case class UDP(source: ByteBufferWrapper) extends Interface
final case class HTTP(source: RequestWrapper) extends Interface