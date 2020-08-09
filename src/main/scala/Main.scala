import zio. { ZIO, App }
import zio.console._
import zio.clock._

import zio.nio._
import zio.nio.channels._

import zio.nio.core._

import zio.stream._


object Gateway extends App {

  def run(args: List[String]) = 
    ZStream.managed(server(8068)).flatMap(handleDatagrams(_))
      .zipWithIndex
      .tap(t => ZIO.when((t._2 + 1) % 20 == 0)(putStrLn((t._2 + 1).toString())))
      .runDrain
      .orDie
      .exitCode

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

}