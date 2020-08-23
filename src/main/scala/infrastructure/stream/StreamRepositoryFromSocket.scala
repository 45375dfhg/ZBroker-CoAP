package infrastructure.stream

import java.io.IOException

import zio.ZEnv

import domain.model.config.ConfigRepository
import domain.model.config.ConfigRepository.ConfigRepository
import domain.model.endpoint.EndpointRepository
import domain.model.endpoint.EndpointRepository.EndpointRepository
import domain.model.stream.StreamRepository
import domain.model.stream.StreamRepository.StreamRepository
import zio.clock._
import zio.{Chunk, Has, Schedule, ZIO, ZLayer}
import zio.nio.channels.DatagramChannel
import zio.nio.core.{Buffer, SocketAddress}
import zio.stream.ZStream
import zio.duration._
import zio.console._

object StreamRepositoryFromSocket extends StreamRepository.Service {

  override def getDatagramStream: ZStream[Has[DatagramChannel] with ConfigRepository, IOException, (Option[SocketAddress], Chunk[Byte])] = {
    ZStream.repeatEffect {
      (for {
        size   <- ConfigRepository.getBufferSize
        buffer <- Buffer.byte(size.value)
        server <- ZIO.service[DatagramChannel]
        origin <- server.receive(buffer)
        _      <- buffer.flip
        chunk  <- buffer.getChunk()
      } yield (origin, chunk)).refineToOrDie[IOException]
    }.take(1)
  }

  override def sendDatagramStream =
    ZStream.repeatEffectWith( {
      (for {
        size   <- ConfigRepository.getBufferSize
        buffer <- Buffer.byte(size.value)
        _      <- buffer.put(5)
        server <- ZIO.service[DatagramChannel]
        adress <- SocketAddress.inetSocketAddress("192.168.137.254", 8080)
        i      <- server.send(buffer, adress)
        _      <- putStrLn("TICK")
      } yield i).refineToOrDie[IOException]
    }, Schedule.spaced(1.second) && Schedule.recurs(10))

  val live: ZLayer[ConfigRepository with Has[DatagramChannel], IOException, Has[StreamRepository.Service]] =
    ZLayer.succeed(this)

}
