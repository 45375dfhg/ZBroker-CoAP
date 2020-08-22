import java.io.IOException

import zio._
import zio.clock.Clock
import zio.{App, Schedule}
import zio.console._
import zio.nio.channels._
import zio.nio.core._
import zio.stream._
import zio.duration._
import zio.Layer

class Port private(val number: Int) extends AnyVal {}

object Port {

  // TODO: proper errors
  def apply(number: Int) =
    Either.cond(number >= 0 && number < 65535, new Port(number), throw new Exception)
}

object PortRepository {

  type PortRepository = Has[PortRepository.Service]

  trait Service {
    def get(): IO[Exception, Port]
  }

  sealed trait ConfigFailure
  object ConfigFailure {
    final case class UnexpectedConfigFailure(err: Throwable) extends ConfigFailure
  }

  // TODO: Needs a proper "No Config Found" error etc.
  def get(): ZIO[PortRepository, Exception, Port] =
    ZIO.accessM(_.get.get())

}

sealed trait Endpoint[R, E, A] {

  val create: ZStream[R, E, A]

}
case object WebSocketEndpoint extends Endpoint[Any, Exception, Nothing] {

  lazy val create = ???

}
/*
 *   TODO: This might need to be changed to a case class to allow multiple sockets
 *   If done so move the Port or ConfigRepository outside but keep it in
 */
import PortRepository.PortRepository // TODO: Needs to be formatted asap!
case object DatagramEndpoint extends Endpoint[PortRepository, Exception, DatagramChannel] {


  private val channel: ZManaged[PortRepository, Exception, DatagramChannel] =
    for {
      port          <- PortRepository.get().toManaged_
      socketAddress <- SocketAddress.inetSocketAddress(port.number).option.toManaged_
      channel       <- DatagramChannel.bind(socketAddress)
    } yield channel

  val create: ZStream[PortRepository, Exception, DatagramChannel] = ZStream.managed(channel)

}

object PortRepositoryInMemory extends PortRepository.Service {

  override def get() = ZIO.fromEither(Port(7025))

  def toLayer: Layer[Nothing, Has[PortRepository.Service]] = ZLayer.succeed(this)
}

object Application extends App {

  val program =
    (for {
      udp  <- DatagramEndpoint.create
      send  = sender(udp).take(1)
      rece  = receiver(udp).take(1)
      _    <- ZStream.mergeAll(2, 16)(send, rece)
    } yield ()).runDrain

  def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    program.provideCustomLayer(PortRepositoryInMemory.toLayer).tap(l => putStrLn(l.toString)).orDie.exitCode
  }

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
}


/*
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

  def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    programm.tap(l => putStrLn(l.toString)).orDie.exitCode
  }

}*/
