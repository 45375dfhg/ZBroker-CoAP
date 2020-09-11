package infrastructure.persistance.chunkstream

import domain.model.chunkstream.ChunkStreamRepository
import domain.model.chunkstream.ChunkStreamRepository.ChunkStreamRepository
import zio.{Chunk, ZLayer}
import zio.stream.ZStream

object ChunkStreamFromMemory extends ChunkStreamRepository.Service {

  // TODO: Implement proper test stub
  override def getChunkStream: ZStream[Any, Nothing, (Int, Chunk[Byte])] = ???
//    ZStream.repeat {
//      val i = scala.util.Random.nextInt(31) + 1
//      val c = Chunk.fromArray(scala.util.Random.nextBytes(i))
//      (i, c)
//    }

}