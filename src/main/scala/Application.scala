import java.io.IOException

import EndpointRepository.EndpointRepository
import StreamRepository.StreamRepository
import zio._
import zio.{App, Schedule}
import zio.console._
import zio.nio.channels._
import zio.nio.core._
import zio.stream._
import zio.duration._
import zio.Layer

class Port private(val number: Int) extends AnyVal {}

sealed trait PortRangeException extends IOException
case object UnexpectedPortRangeException extends PortRangeException

object Port {

  def apply(number: Int): Either[PortRangeException, Port] =
    Either.cond(1 to 65535 contains number, new Port(number), UnexpectedPortRangeException)

}

class BufferSize private(val value: Int) extends AnyVal {}

sealed trait BufferSizeException extends IOException
case object UnexpectedBufferSizeException extends BufferSizeException

object BufferSize {

  def apply(value: Int): Either[BufferSizeException, BufferSize] =
    Either.cond(1 to 65535 contains value, new BufferSize(value), UnexpectedBufferSizeException)
}

object ConfigRepository {

  type ConfigRepository = Has[ConfigRepository.Service]

  trait Service {
    val getPrimaryUDPPort: IO[PortRangeException, Port]
    val getBufferSize: IO[BufferSizeException, BufferSize]
  }

  lazy val getPrimaryUDPPort: ZIO[ConfigRepository, PortRangeException, Port] =
    ZIO.accessM(_.get.getPrimaryUDPPort)

  lazy val getBufferSize: ZIO[ConfigRepository, BufferSizeException, BufferSize] =
    ZIO.accessM(_.get.getBufferSize)

}

object ConfigRepositoryInMemory extends ConfigRepository.Service {

  // TODO: Configure a proper fallback
  override lazy val getPrimaryUDPPort: IO[PortRangeException, Port] = ZIO.fromEither(Port(7025))

  override lazy val getBufferSize: IO[BufferSizeException, BufferSize] = ZIO.fromEither(BufferSize(100))

  def live: Layer[Nothing, Has[ConfigRepository.Service]] = ZLayer.succeed(this)
}

/*
 * TODO: Wrapping this might be necessary to do proper apply() calls?
 */
// sealed trait Endpoint
// final case class WebSocketEndpoint extends Endpoint
// final case class DatagramEndpoint(channel: DatagramChannelWrapper) extends Endpoint
// class DatagramChannelWrapper(val underlying: DatagramChannel) extends AnyVal

object EndpointRepository {
  import ConfigRepository.ConfigRepository

  type EndpointRepository = Has[EndpointRepository.Service] with Has[ConfigRepository.Service]

  trait Service {
    def getDatagramEndpoint: ZManaged[EndpointRepository, IOException, DatagramChannel]
  }

  def getDatagramEndpoint: ZManaged[EndpointRepository, IOException, DatagramChannel] =
    ZManaged.accessManaged(_.get.getDatagramEndpoint)

}

object EndpointRepositoryInMemory extends EndpointRepository.Service {
  import ConfigRepository.ConfigRepository

  private lazy val datagramChannel: ZManaged[ConfigRepository, IOException, DatagramChannel] =
    for {
      port          <- ConfigRepository.getPrimaryUDPPort.toManaged_
      socketAddress <- SocketAddress.inetSocketAddress(port.number).option.toManaged_
      channel       <- DatagramChannel.bind(socketAddress)
  } yield channel

  override def getDatagramEndpoint: ZManaged[ConfigRepository, IOException, DatagramChannel] = datagramChannel

  def live: ZLayer[ConfigRepository, IOException, Has[EndpointRepository.Service]] = ZLayer.succeed(this)
}

object StreamRepository {
  import EndpointRepository.EndpointRepository

  type StreamRepository = Has[StreamRepository.Service] with EndpointRepository

  trait Service {
    val getDatagramStream: ZStream[StreamRepository, IOException, (Option[SocketAddress], Chunk[Byte])]
  }

  lazy val getDatagramStream: ZStream[StreamRepository, IOException, (Option[SocketAddress], Chunk[Byte])] =
    ZStream.accessStream(_.get.getDatagramStream)
}

object StreamRepositoryInMemory extends StreamRepository.Service {

  def fetchDatagram(server: DatagramChannel) =
    ZStream.repeatEffect {
      (for {
        size   <- ConfigRepository.getBufferSize
        buffer <- Buffer.byte(size.value)
        origin <- server.receive(buffer)
        _      <- buffer.flip
        chunk  <- buffer.getChunk()
      } yield (origin, chunk)).refineToOrDie[IOException]
    }

  override lazy val getDatagramStream: ZStream[StreamRepository, IOException, (Option[SocketAddress], Chunk[Byte])] =
    ZStream.managed(EndpointRepository.getDatagramEndpoint).flatMap(fetchDatagram)

  def live: ULayer[Has[StreamRepositoryInMemory.type]] = ZLayer.succeed(this)
}

object Application extends App {

  val program =
    (for {
      _    <- ZStream.fromEffect(putStrLn("booting up ..."))
      udp  <- ZStream.managed(EndpointRepository.getDatagramEndpoint)
      send  = sender(udp).take(1)
      rece  = receiver(udp).take(1)
      _    <- ZStream.mergeAll(2, 16)(send, rece)
    } yield ()).runDrain

  val layer = ConfigRepositoryInMemory.live >+> EndpointRepositoryInMemory.live

  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    program.tap(l => putStrLn(l.toString)).provideCustomLayer(layer).orDie.exitCode

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

//sealed trait Endpoint[R, E, A] {
//
//  val create: ZStream[R, E, A]
//
//}
//case object WebSocketEndpoint extends Endpoint[Any, Exception, Nothing] {
//
//  lazy val create = ???
//
//}
//
//import ConfigRepository.ConfigRepository // TODO: Needs to be formatted asap!
//case object DatagramEndpoint extends Endpoint[ConfigRepository, Exception, DatagramChannel] {
//
//  private val channel: ZManaged[ConfigRepository, Exception, DatagramChannel] =
//    for {
//      port          <- ConfigRepository.getPrimaryUDPPort.toManaged_
//      socketAddress <- SocketAddress.inetSocketAddress(port.number).option.toManaged_
//      channel       <- DatagramChannel.bind(socketAddress)
//    } yield channel
//
//  val create: ZStream[ConfigRepository, Exception, DatagramChannel] = ZStream.managed(channel)
//
//}





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
