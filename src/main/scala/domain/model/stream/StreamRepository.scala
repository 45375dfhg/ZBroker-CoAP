package domain.model.stream

import java.io.IOException

import domain.model.config.ConfigRepository.ConfigRepository
import zio.clock.Clock
import zio.console.Console
import zio.{Chunk, Has}
import zio.nio.channels.DatagramChannel
import zio.nio.core.SocketAddress
import zio.stream.ZStream

object StreamRepository {

  type StreamRepository = Has[StreamRepository.Service]

  trait Service {
    def getDatagramStream(c: DatagramChannel): ZStream[ConfigRepository, IOException, (Option[SocketAddress], Chunk[Byte])]
    def sendDatagramStream(c: DatagramChannel): ZStream[ConfigRepository with Console with Clock, IOException, Int]
  }

  def getDatagramStream(c: DatagramChannel): ZStream[StreamRepository with ConfigRepository, IOException, (Option[SocketAddress], Chunk[Byte])] =
    ZStream.accessStream(_.get.getDatagramStream(c))

  def sendDatagramStream(c: DatagramChannel): ZStream[StreamRepository with ConfigRepository with Console with Clock, IOException, Int] =
    ZStream.accessStream(_.get.sendDatagramStream(c))

}
