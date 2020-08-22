package infrastructure.stream

import java.io.IOException

import domain.model.config.ConfigRepository
import domain.model.config.ConfigRepository.ConfigRepository
import domain.model.stream.StreamRepository
import zio.clock.Clock
import zio.{Chunk, Has, Schedule, ZLayer}
import zio.nio.channels.DatagramChannel
import zio.nio.core.{Buffer, SocketAddress}
import zio.stream.ZStream
import zio.duration._
import zio.console._

object StreamRepositoryFromSocket extends StreamRepository.Service {

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

    def sendDatagram(server: DatagramChannel) =
      ZStream.repeatEffectWith( {
        (for {
          size   <- ConfigRepository.getBufferSize
          buffer <- Buffer.byte(size.value)
          _      <- buffer.put(5)
          adress <- SocketAddress.inetSocketAddress("127.0.0.1", 8080)
          i      <- server.send(buffer, adress)
        } yield i).refineToOrDie[IOException]
      }, Schedule.spaced(1.second) && Schedule.recurs(10))

  override def getDatagramStream(c: DatagramChannel): ZStream[ConfigRepository, IOException, (Option[SocketAddress], Chunk[Byte])] =
    ZStream(c).flatMap(fetchDatagram)

  override def sendDatagramStream(c: DatagramChannel): ZStream[ConfigRepository with Console with Clock, IOException, Int] =
    ZStream(c).flatMap(sendDatagram)

  def live: ZLayer[ConfigRepository, IOException, Has[StreamRepository.Service]] = ZLayer.succeed(this)
}
