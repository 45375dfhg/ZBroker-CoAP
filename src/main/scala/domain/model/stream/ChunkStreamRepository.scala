package domain.model.stream

import java.io.IOException
import zio.{Chunk, Has}
import zio.stream.ZStream

object ChunkStreamRepository {

  type ChunkStreamRepository = Has[ChunkStreamRepository.Service]

  trait Service {
    def getStream: ZStream[Any, IOException, (Any, Chunk[Byte])]
  }

  def getStream: ZStream[ChunkStreamRepository, IOException, (Any, Chunk[Byte])] =
    ZStream.accessStream(_.get.getStream)
}