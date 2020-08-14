import zio. { ZIO, App, ZEnv, ExitCode, Chunk, ZLayer, Has, RIO, IO }

import zio.console._

import zio.nio._


import zio.nio.core.channels._

// import zio.nio.channels._


import zio.nio.core._

import zio.stream._

import java.io.IOException

object Gateway extends App {

  def nioBlocking[A, C <: Channel](
    channel: C
  )(f: C => ZIO[zio.blocking.Blocking, Exception, A]): ZIO[zio.blocking.Blocking, Exception, A] =
    zio.blocking.blocking(f(channel)).fork.flatMap(_.join).onInterrupt(channel.close.ignore)
  
  val programm = 
    ZStream.managed(server(8068))
      .flatMap(handleDatagrams(_))
      .runDrain 

  def server(port: Int) = {
    for {
      server        <- DatagramChannel.open.toManaged_
      socketAddress <- SocketAddress.inetSocketAddress(port).option.toManaged_
      _             <- server.bind(socketAddress).toManaged_
    } yield server
  } 
  
  def handleDatagrams(server: DatagramChannel) = 
    ZStream.repeatEffect {
      for {
        buffer <- Buffer.byte(16)
        _      <- nioBlocking(server)(_.receive(buffer))
        _      <- buffer.flip
        chunk  <- buffer.getChunk()
      } yield chunk
    }

  def run(args: List[String]) =
    programm.orDie.exitCode

}