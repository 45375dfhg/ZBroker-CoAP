import zio. { ZIO, App, console }

import zio.nio.core._
import zio.nio.core.channels._

import zio.stream._


object MyApp extends App {

  def run(args: List[String]) =
    ZStream.managed(server(8080)).flatMap(handleDatagrams(_)).runDrain.orDie.exitCode

  def server(port: Int) =
     for {
      server        <- DatagramChannel.open.toManaged_
      socketAddress <- SocketAddress.inetSocketAddress(port).asSome.toManaged_
      _             <- server.bind(socketAddress).toManaged_
    } yield server

  def handleDatagrams(server: DatagramChannel) = 
    ZStream.repeatEffect(Buffer.byte(128) >>= server.receive)

}