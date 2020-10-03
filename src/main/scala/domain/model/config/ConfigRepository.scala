package domain.model.config

import domain.model.BufferModel._
import domain.model.FiberModel._
import domain.model.PortModel._
import zio._

object ConfigRepository {

  type ConfigRepository = Has[ConfigRepository.Service]

  trait Service {
    def getPrimaryUDPPort: IO[PortRangeException, Port]
    def getInwardBufferSize: IO[BufferSizeException, BufferSize]
    def getOutwardBufferSize: IO[BufferSizeException, BufferSize]
    def getStreamFiberAmount: IO[FiberNumberException, FiberNumber]
  }

  def getPrimaryUDPPort: ZIO[ConfigRepository, PortRangeException, Port] =
    ZIO.accessM(_.get.getPrimaryUDPPort)

  def getInwardBufferSize: ZIO[ConfigRepository, BufferSizeException, BufferSize] =
    ZIO.accessM(_.get.getInwardBufferSize)

  def getOutwardBufferSize: ZIO[ConfigRepository, BufferSizeException, BufferSize] =
    ZIO.accessM(_.get.getOutwardBufferSize)

  def getStreamFiberAmount: ZIO[ConfigRepository, FiberNumberException, FiberNumber] =
    ZIO.accessM(_.get.getStreamFiberAmount)
}