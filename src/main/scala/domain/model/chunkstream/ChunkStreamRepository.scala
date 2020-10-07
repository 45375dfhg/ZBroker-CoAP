package domain.model.chunkstream

import domain.model.config.ConfigRepository.ConfigRepository
import domain.model.exception.GatewayError
import zio.nio.core.SocketAddress
import zio.nio.core.channels.DatagramChannel
import zio._
import zio.console.Console
import zio.stream._

object ChunkStreamRepository {

  type Channel = Has[DatagramChannel]

  type ChunkStreamRepository = Has[ChunkStreamRepository.Service]

  type DatagramDump = (Option[SocketAddress], Chunk[Byte])

  trait Service {
    def getChunkStream: ZStream[ConfigRepository with Channel with Console, GatewayError, DatagramDump]
  }

  def getChunkStream: ZStream[ChunkStreamRepository with ConfigRepository with Channel with Console, GatewayError, DatagramDump] =
    ZStream.accessStream(_.get.getChunkStream)
}