import domain.model.stream.StreamRepository
import infrastructure.endpoint.EndpointRepositoryInMemory
import infrastructure.environments.ConfigRepositoryInMemory
import infrastructure.stream.StreamRepositoryInMemory

import zio._
import zio.{App, Schedule}
import zio.console._
import zio.nio.channels._
import zio.nio.core._
import zio.stream._
import zio.duration._

object Application extends App {

  val program =
    (for {
      _    <- ZStream.fromEffect(putStrLn("booting up ..."))
      a    <- StreamRepository.getDatagramStream.take(1000000)
      // udp  <- ZStream.managed(EndpointRepository.getDatagramEndpoint)
      // send  = sender(udp).take(1)
      // rece  = receiver(udp).take(1)
      // _    <- ZStream.mergeAll(2, 16)(send, rece)
    } yield ()).runDrain

  val layer = (ConfigRepositoryInMemory.live
    >+> EndpointRepositoryInMemory.live
    >+> StreamRepositoryInMemory.live)

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
