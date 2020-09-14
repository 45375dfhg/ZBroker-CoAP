package domain.model.chunkstream

import domain.model.config.ConfigRepository.ConfigRepository
import domain.model.exception.GatewayError
import zio.nio.core.SocketAddress
import zio.nio.core.channels.DatagramChannel
import zio.{Chunk, Has}
import zio.stream.ZStream

object ChunkStreamRepository {

  type ChunkStreamRepository = Has[ChunkStreamRepository.Service]

  type DatagramDump = (Option[SocketAddress], Chunk[Byte])

  trait Service {
    def getChunkStream: ZStream[ConfigRepository with Has[DatagramChannel], GatewayError, DatagramDump]
  }

  def getChunkStream: ZStream[ChunkStreamRepository with ConfigRepository with Has[DatagramChannel], GatewayError, DatagramDump] =
    ZStream.accessStream(_.get.getChunkStream)
}