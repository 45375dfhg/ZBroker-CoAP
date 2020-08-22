import zio._
import zio.clock.Clock
import zio.{App, Schedule}
import zio.console._
import zio.nio.channels._
import zio.nio.core._
import zio.stream._
import zio.duration._

object Application extends App {

  val routine =
    ZStream.managed(for {
        // server        <- DatagramChannel.open
        socketAddress <- SocketAddress.inetSocketAddress(7025).option.toManaged_
        channel       <- DatagramChannel.bind(socketAddress)
    } yield channel)
      
  def sender(stream: DatagramChannel) =
    ZStream(stream).flatMap(sendDatamgrams)

  def receiver(stream: DatagramChannel) =
    ZStream(stream).flatMap(handleDatagrams)

  def handleDatagrams(server: DatagramChannel) =
    ZStream.repeatEffect {
      for {
        buffer <- Buffer.byte(20)
        a      <- server.receive(buffer)
        _      <- buffer.flip
        chunk  <- buffer.getChunk()
      } yield chunk
    }

  def sendDatamgrams(server: DatagramChannel) =
    ZStream.repeatEffectWith( {
      for {
        buffer <- Buffer.byte(20)
        _      <- buffer.put(5)
        adress <- SocketAddress.inetSocketAddress("127.0.0.1", 8080)
        i      <- server.send(buffer, adress)
      } yield i
    }, Schedule.fixed(1.second) ++ Schedule.recurs(1))
  
  val programm = 
    (for {
      channel <- routine
      send    =  sender(channel).take(1)
      rece    =  receiver(channel).take(1)
      _       <- ZStream.mergeAll(2, 16)(send, rece)
    } yield()).runDrain

  def run(args: List[String]) = {
    programm.tap(l => putStrLn(l.toString)).orDie.exitCode
  }

}