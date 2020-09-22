package infrastructure.persistance.config

import domain.model.BufferModel._
import domain.model.PortModel._
import domain.model.config._
import zio._

object ConfigRepositoryInMemory extends ConfigRepository.Service {

  // TODO: Configure proper fallbacks
  override def getPrimaryUDPPort: IO[PortRangeException, Port] = Port(7020)

  override def getInwardBufferSize: IO[BufferSizeException, BufferSize] = BufferSize(100)

  override def getOutwardBufferSize: IO[BufferSizeException, BufferSize] = BufferSize(100)
}