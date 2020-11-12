package infrastructure.persistance.chunkstream

import domain.model.chunkstream.ChunkStreamRepository
import zio.nio.core.SocketAddress
import zio.Chunk
import zio.stream.ZStream

object ChunkStreamFromMemory extends ChunkStreamRepository.Service {

  // Entry Point for a test-stub
  override def getChunkStream: ZStream[Any, Nothing, (Option[SocketAddress], Chunk[Byte])] = ???

}