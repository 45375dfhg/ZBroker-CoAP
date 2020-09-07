package domain.model.chunkstream

import java.io.IOException

import domain.model.config.ConfigRepository.ConfigRepository

import zio.nio.core.channels.DatagramChannel
import zio.{Chunk, Has}
import zio.stream.ZStream

object ChunkStreamRepository {

  type ChunkStreamRepository = Has[ChunkStreamRepository.Service]

  trait Service {
    def getChunkStream: ZStream[ConfigRepository with Has[DatagramChannel], IOException, (Any, Chunk[Byte])]
  }

  def getChunkStream: ZStream[ChunkStreamRepository with ConfigRepository with Has[DatagramChannel], IOException, (Any, Chunk[Byte])] =
    ZStream.accessStream(_.get.getChunkStream)
}