package domain.model.stream

import java.io.IOException

import domain.model.config.ConfigRepository.ConfigRepository

import zio.nio.channels.DatagramChannel
import zio.{Chunk, Has}
import zio.stream.ZStream

object ChunkStreamRepository {

  type ChunkStreamRepository = Has[ChunkStreamRepository.Service]

  trait Service {
    def getStream: ZStream[ConfigRepository with Has[DatagramChannel], IOException, (Any, Chunk[Byte])]
  }

  def getStream: ZStream[ChunkStreamRepository with ConfigRepository with Has[DatagramChannel], IOException, (Any, Chunk[Byte])] =
    ZStream.accessStream(_.get.getStream)
}