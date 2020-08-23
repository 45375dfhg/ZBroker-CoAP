package domain.model.stream

import java.io.IOException

import domain.model.config.ConfigRepository.ConfigRepository
import zio.clock.Clock
import zio.{Chunk, Has}
import zio.nio.channels.DatagramChannel
import zio.nio.core.SocketAddress
import zio.stream.ZStream
import zio.console._

object StreamRepository {

  type StreamRepository = Has[StreamRepository.Service]
  type CC = Has[DatagramChannel] with ConfigRepository

  trait Service {
    def getDatagramStream: ZStream[Has[DatagramChannel] with ConfigRepository, IOException, (Option[SocketAddress], Chunk[Byte])]
    def sendDatagramStream: ZStream[Has[DatagramChannel] with ConfigRepository with Clock with Console, IOException, Int]
  }

  def getDatagramStream: ZStream[StreamRepository with Has[DatagramChannel] with ConfigRepository, IOException, (Option[SocketAddress], Chunk[Byte])] =
    ZStream.accessStream(_.get.getDatagramStream)

  def sendDatagramStream: ZStream[StreamRepository with Has[DatagramChannel] with ConfigRepository with Clock with Console, IOException, Int] =
    ZStream.accessStream(_.get[Service].sendDatagramStream)
}
