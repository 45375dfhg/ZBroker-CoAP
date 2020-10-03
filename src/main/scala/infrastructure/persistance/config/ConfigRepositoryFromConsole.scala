package infrastructure.persistance.config

import domain.model.BufferModel._
import domain.model.FiberModel._
import domain.model.PortModel._
import domain.model.config._

import zio._

// TODO: will be created in place - so remove later!
object ConfigRepositoryFromConsole extends ConfigRepository.Service {

  override def getPrimaryUDPPort: IO[PortRangeException, Port] = Port(5683)

  override def getInwardBufferSize: IO[BufferSizeException, BufferSize] = BufferSize(100)

  override def getOutwardBufferSize: IO[BufferSizeException, BufferSize] = BufferSize(100)

  override def getStreamFiberAmount: IO[FiberNumberException, FiberNumber] = FiberNumber(16)
}