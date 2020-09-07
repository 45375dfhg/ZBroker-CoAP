package infrastructure.chunkstream

import java.io.IOException

import domain.model.config.ConfigRepository
import domain.model.config.ConfigRepository.ConfigRepository
import domain.model.chunkstream.ChunkStreamRepository
import domain.model.chunkstream.ChunkStreamRepository.ChunkStreamRepository

import zio.{Chunk, Has, ZIO, ZLayer}
import zio.nio.core.channels.DatagramChannel
import zio.nio.core.{Buffer, SocketAddress}
import zio.stream.ZStream
import zio.console._

object ChunkStreamFromSocket extends ChunkStreamRepository.Service {

  override def getChunkStream:
  ZStream[ConfigRepository with Has[DatagramChannel], IOException, (Option[SocketAddress], Chunk[Byte])] =
    ZStream.repeatEffect {
      (for {
        size   <- ConfigRepository.getBufferSize
        buffer <- Buffer.byte(size.value)
        server <- ZIO.service[DatagramChannel]
        origin <- server.receive(buffer)
        _      <- buffer.flip
        chunk  <- buffer.getChunk()
      } yield (origin, chunk)).refineToOrDie[IOException]
    }

  val live: ZLayer[Any, IOException, ChunkStreamRepository] = ZLayer.succeed(this)
}