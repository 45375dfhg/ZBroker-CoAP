package domain.model.config

import domain.model.BufferModel._
import domain.model.FiberModel._
import domain.model.PortModel._
import zio._
import zio.console.Console

object ConfigRepository {

  type ConfigRepository = Has[ConfigRepository.Service]

  trait Service {
    def getPrimaryUDPPort: ZIO[Console, PortRangeException, Port]
    def getInwardBufferSize: ZIO[Console, BufferSizeException, BufferSize]
    def getOutwardBufferSize: ZIO[Console, BufferSizeException, BufferSize]
    def getStreamFiberAmount: ZIO[Console, FiberNumberException, FiberNumber]
  }

  def getPrimaryUDPPort: ZIO[ConfigRepository with Console, PortRangeException, Port] =
    ZIO.accessM(_.get.getPrimaryUDPPort)

  def getInwardBufferSize: ZIO[ConfigRepository with Console, BufferSizeException, BufferSize] =
    ZIO.accessM(_.get.getInwardBufferSize)

  def getOutwardBufferSize: ZIO[ConfigRepository with Console, BufferSizeException, BufferSize] =
    ZIO.accessM(_.get.getOutwardBufferSize)

  def getStreamFiberAmount: ZIO[ConfigRepository with Console, FiberNumberException, FiberNumber] =
    ZIO.accessM(_.get.getStreamFiberAmount)
}