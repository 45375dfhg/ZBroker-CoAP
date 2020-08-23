package infrastructure.stream

import java.io.IOException

import domain.model.config.ConfigRepository
import domain.model.stream.ChunkStreamRepository
import domain.model.stream.ChunkStreamRepository.ChunkStreamRepository
import infrastructure.config.ConfigRepositoryInMemory
import infrastructure.endpoint.EndpointRepositoryFromSocket
import zio.{Chunk, ZIO, ZLayer}
import zio.nio.channels.DatagramChannel
import zio.nio.core.{Buffer, SocketAddress}
import zio.stream.ZStream


object ChunkStreamFromSocket extends ChunkStreamRepository.Service {

  val layer = ConfigRepositoryInMemory.live >+> EndpointRepositoryFromSocket.socketLayer

  override def getStream: ZStream[Any, IOException, (Option[SocketAddress], Chunk[Byte])] =
    ZStream.repeatEffect {
      (for {
        size   <- ConfigRepository.getBufferSize
        buffer <- Buffer.byte(size.value)
        server <- ZIO.service[DatagramChannel]
        origin <- server.receive(buffer)
        _      <- buffer.flip
        chunk  <- buffer.getChunk()
      } yield (origin, chunk)).refineToOrDie[IOException]
    }.provideLayer(layer)

  val live: ZLayer[Any, IOException, ChunkStreamRepository] = ZLayer.succeed(this)
}