package domain.model.stream

import java.io.IOException

import domain.model.config.ConfigRepository.ConfigRepository

import zio.nio.core.channels.DatagramChannel
import zio.{Chunk, Has}
import zio.stream.ZStream

object ChunkStreamRepository {

  type ChunkStreamRepository = Has[ChunkStreamRepository.Service]

  trait Service {
    def getStream: ZStream[ConfigRepository with Has[DatagramChannel], IOException, (Any, Chunk[Boolean])]
  }

  def getStream: ZStream[ChunkStreamRepository with ConfigRepository with Has[DatagramChannel], IOException, (Any, Chunk[Boolean])] =
    ZStream.accessStream(_.get.getStream)
}