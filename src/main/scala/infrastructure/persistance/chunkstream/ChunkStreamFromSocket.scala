package infrastructure.persistance.chunkstream

import domain.model.config._
import domain.model.config.ConfigRepository._
import domain.model.chunkstream._
import domain.model.exception._
import zio._
import zio.console.Console
import zio.nio.core.channels._
import zio.nio.core._
import zio.stream._

object ChunkStreamFromSocket extends ChunkStreamRepository.Service {

  override def getChunkStream: ZStream[ConfigRepository with Has[DatagramChannel] with Console, GatewayError, (Option[SocketAddress], Chunk[Byte])] =
    ZStream.repeatEffect {
      (for {
        size   <- ConfigRepository.getInwardBufferSize
        buffer <- Buffer.byte(size.value)
        server <- ZIO.service[DatagramChannel]
        origin <- server.receive(buffer)
        _      <- buffer.flip
        chunk  <- buffer.getChunk()
      } yield (origin, chunk)).refineToOrDie[GatewayError]
    }

}