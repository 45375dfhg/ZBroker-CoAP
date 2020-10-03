package infrastructure.persistance.config

import domain.model.BufferModel._
import domain.model.FiberModel._
import domain.model.PortModel._
import domain.model.config.ConfigRepository._
import domain.model.config._

import zio._
import zio.console._

import scala.util.Try

// TODO: will be created in place - so remove later!
object ConfigRepositoryFromConsole extends ConfigRepository.Service {

  override def getPrimaryUDPPort: IO[PortRangeException, Port] = Port(5683)

  override def getInwardBufferSize: IO[BufferSizeException, BufferSize] = BufferSize(100)

  override def getOutwardBufferSize: IO[BufferSizeException, BufferSize] = BufferSize(100)

  override def getStreamFiberAmount: IO[FiberNumberException, FiberNumber] = FiberNumber(16)

  val live: ZLayer[Any, Nothing, ConfigRepository] = ZLayer.succeed {

    new Service {

      override def getPrimaryUDPPort: URIO[Console, Port] =
        (for {
          input <- putStrLn("Enter the port number to which CoAP services connect:") *> getStrLn
          port  <- IO.fromTry(Try(input.toInt)).flatMap(Port(_))
        } yield port).foldM(_ => putStrLn("Not a valid port number.") *> getPrimaryUDPPort, UIO.succeed(_))

      override def getInwardBufferSize = ???

      override def getOutwardBufferSize = ???

      override def getStreamFiberAmount = ???
    }
  }
}
