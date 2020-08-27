package infrastructure.stream

import domain.model.stream.ChunkStreamRepository
import domain.model.stream.ChunkStreamRepository.ChunkStreamRepository
import zio.{Chunk, ZLayer}
import zio.stream.ZStream

object ChunkStreamFromMemory extends ChunkStreamRepository.Service {

  // TODO: Implement proper test stub
  override def getStream: ZStream[Any, Nothing, (Int, Chunk[Boolean])] = ???
//    ZStream.repeat {
//      val i = scala.util.Random.nextInt(31) + 1
//      val c = Chunk.fromArray(scala.util.Random.nextBytes(i))
//      (i, c)
//    }

  val live: ZLayer[Any, Nothing, ChunkStreamRepository] = ZLayer.succeed(this)
}