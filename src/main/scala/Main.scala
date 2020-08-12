import zio. { ZIO, App, ZEnv, ExitCode }

import zio.nio._
import zio.nio.channels._

import zio.nio.core._

import zio.stream._

object Gateway extends App {
  
  val programm = 
    ZStream.managed(server(8068))
      .flatMap(handleDatagrams(_))
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
        _      <- buffer.flip
        chunk  <- buffer.getChunk()
      } yield chunk
    }

  def run(args: List[String]) =
    programm.orDie.exitCode

}