import zio. { ZIO, App, ZEnv, ExitCode }
import zio.console._
import zio.clock._

import zio.nio._
import zio.nio.channels._

import zio.nio.core._

import zio.stream._

object Gateway extends App {
  
  val programm = 
    ZStream.managed(server(8068)).flatMap(handleDatagrams(_))
      .zipWithIndex
      .tap(t => ZIO.when((t._2 + 1) % 20 == 0)(putStrLn((t._2 + 1).toString())))
      .runDrain 

  def server(port: Int) = {
    for {
      socketAddress <- SocketAddress.inetSocketAddress(port).option.toManaged_
      server        <- DatagramChannel.bind(socketAddress)
    } yield server
  } 
  
  def handleDatagrams(server: DatagramChannel) = 
    ZStream.repeatEffect {
      for {
        buffer <- Buffer.byte(16)
        _      <- server.receive(buffer)
        fiber  <- (buffer.flip *> buffer.getChunk()).fork
        chunk  <- fiber.join
      } yield chunk
    }

  def run(args: List[String]) =
    programm.orDie.exitCode

}