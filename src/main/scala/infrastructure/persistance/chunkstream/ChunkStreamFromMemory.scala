package infrastructure.persistance.chunkstream

import domain.model.chunkstream.ChunkStreamRepository
import zio.nio.core.SocketAddress
import zio.Chunk
import zio.stream.ZStream

object ChunkStreamFromMemory extends ChunkStreamRepository.Service {

  // TODO: Implement proper test stub
  override def getChunkStream: ZStream[Any, Nothing, (Option[SocketAddress], Chunk[Byte])] = ???
//    ZStream.repeat {
//      val i = scala.util.Random.nextInt(31) + 1
//      val c = Chunk.fromArray(scala.util.Random.nextBytes(i))
//      (i, c)
//    }

}