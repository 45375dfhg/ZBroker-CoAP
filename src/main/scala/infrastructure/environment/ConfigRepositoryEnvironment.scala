package infrastructure.environment

import domain.model.BufferModel.BufferSize
import domain.model.FiberModel.FiberNumber
import domain.model.PortModel.Port
import domain.model.config.ConfigRepository.{ConfigRepository, Service}
import domain.model.config._
import infrastructure.persistance.config._
import zio._
import zio.console.{Console, putStrLn}

import scala.util.Try

object ConfigRepositoryEnvironment {

  val fromMemory: ULayer[Has[ConfigRepository.Service]] = ZLayer.succeed(ConfigRepositoryInMemory)

  // val fromConsole: ULayer[Has[ConfigRepository.Service]] = ZEnv.live >+> ConfigRepositoryFromConsole.live

//  val fromConsole: ZLayer[Any, Nothing, ConfigRepository] = ZLayer.fromFunction { (console: Console) =>
//
//    new Service {
//      // TODO: ALL THESE 3 ARE BASICALLY THE SAME FUNCTION!
//      override def getPrimaryUDPPort: URIO[Console, Port] =
//        (for {
//          input <- console.get.putStr("[CONFIG] Enter the port number to which CoAP services connect: ") *> console.get.getStrLn
//          port  <- IO.fromTry(Try(input.toInt)).flatMap(Port(_))
//        } yield port).foldM(_ => console.get.putStrLn("[CONFIG-ERROR] Not a valid port number.") *> getPrimaryUDPPort, UIO.succeed(_))
//
//      override def getInwardBufferSize: URIO[Console, BufferSize] =
//        (for {
//          input <- console.get.putStr("[CONFIG] Enter the size of possible incoming packets: ") *> console.get.getStrLn
//          port  <- IO.fromTry(Try(input.toInt)).flatMap(BufferSize(_))
//        } yield port).foldM(_ => console.get.putStrLn("[CONFIG-ERROR] Not a valid buffer size.") *> getInwardBufferSize, p => putStrLn(s"$p set succesfully") *> UIO.succeed(p) )
//
//      override def getStreamFiberAmount: URIO[Console, FiberNumber] =
//        (for {
//          input <- console.get.putStr("[CONFIG] How many fibers should the stream use: ") *> console.get.getStrLn
//          port  <- IO.fromTry(Try(input.toInt)).flatMap(FiberNumber(_))
//        } yield port).foldM(_ => console.get.putStrLn("[CONFIG-ERROR] Not a valid fiber number.") *> getStreamFiberAmount, UIO.succeed(_))
//    }
//  } ++ ZEnv.live
}