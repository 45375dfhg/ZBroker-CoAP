import zio. { ZIO, App }
import zio.console._
import zio.clock._

import zio.nio._
import zio.nio.channels._

import zio.nio.core._

import zio.stream._
import zio.ExitCode
import zio.Ref


object NonStream extends App {

  def run(args: List[String]) = 
    programm().useForever.orDie.exitCode
    
  def programm() =
    for {
        server <- server(8068)
        ref    <- Ref.make(0).toManaged_
        _      <- handleDatagrams(server, ref).fork.forever.toManaged_
    } yield ()
    
  def server(port: Int) = 
    for {
        socketAddress <- SocketAddress.inetSocketAddress(port).option.toManaged_
        server        <- DatagramChannel.bind(socketAddress)
    } yield server
    
  def handleDatagrams(server: DatagramChannel, ref: Ref[Int]) = 
    for {
        buffer <- Buffer.byte(16)
        _      <- server.receive(buffer)
        _      <- buffer.flip
        c      <- buffer.getChunk().fork
        r      <- ref.updateAndGet(_ + 1)
        _      <- ZIO.when(r % 10000 == 0)(putStrLn(r.toString()))
        res      <- c.join
      } yield res


}