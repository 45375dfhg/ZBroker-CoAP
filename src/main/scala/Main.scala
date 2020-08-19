import zio. { ZIO, App, ZEnv, ExitCode, Chunk, ZLayer, Has, RIO, IO, URIO }

import zio.console._

import zio.nio._
import zio.nio.channels._

import zio.nio.core._

import zio.stream._

import java.io.IOException
import zio.Schedule
import zio.duration._

object Main extends App {

  /* def nioBlocking[A, C <: Channel](
    channel: C
  )(f: C => ZIO[zio.blocking.Blocking, Exception, A]): ZIO[zio.blocking.Blocking, Exception, A] =
    zio.b locking.blocking(f(channel)).fork.flatMap(_.join).onInterrupt(channel.close.ignore)
    */

  val routine =
    ZStream.managed(for {
        // server        <- DatagramChannel.open
        socketAddress <- SocketAddress.inetSocketAddress(7025).option.toManaged_
        channel       <- DatagramChannel.bind(socketAddress)
    } yield channel)
      
  def sender(stream: DatagramChannel) = 
    ZStream(stream).flatMap(sendDatamgrams(_))

  def receiver(stream: DatagramChannel) = 
    ZStream(stream).flatMap(handleDatagrams(_))

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
    programm.tap(l => putStrLn(l.toString())).orDie.exitCode
  }

}

/* import zio. { ZIO, App, ZEnv, ExitCode, Chunk, ZLayer, Has, RIO, IO, URIO }

import zio.console._

import zio.nio._
import zio.nio.core.channels._

import zio.nio.core._

import zio.stream._

import java.io.IOException
import java.net.Socket
import zio.Schedule
import zio.duration._

object Gateway extends App {

  class ExtendedChannel(channel: DatagramChannel) {
    def receive_(dst: ByteBuffer) = channel.receive(dst).fork.flatMap(_.join).onInterrupt(channel.close.ignore)
  }

  implicit def extendDatagramChannel(channel: DatagramChannel) = new ExtendedChannel(channel)
  
  val routine =
    ZStream.fromEffect(for {
        server        <- DatagramChannel.open
        socketAddress <- SocketAddress.inetSocketAddress(7075).option
        channel       <- server.bind(socketAddress)
    } yield channel)
      
  def sender(stream: DatagramChannel) = 
    ZStream(stream).flatMap(sendDatamgrams(_)) //.tap(_ => putStrLn("SENDER"))

  def receiver(stream: DatagramChannel) = 
    ZStream(stream).flatMap(handleDatagrams(_)) //.tap(_ => putStrLn("RECEIV"))

  def handleDatagrams(server: DatagramChannel) = 
    ZStream.repeatEffect {
      for {
        buffer <- Buffer.byte(20)
        res    <- server.receive_(buffer)
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
    }, Schedule.fixed(1.second) ++ Schedule.recurs(10))
  
  val programm = 
    (for {
      channel <- routine
      send    =  sender(channel).take(1)
      rece    =  receiver(channel).take(1)
      _       <- ZStream.mergeAll(2, 16)(send, rece)
    } yield()).runDrain

  def run(args: List[String]) = {
    programm.tap(l => putStrLn(l.toString())).orDie.exitCode
  }

} */