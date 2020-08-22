package infrastructure.stream

import java.io.IOException

import domain.model.config.ConfigRepository
import domain.model.config.ConfigRepository.ConfigRepository
import domain.model.endpoint.EndpointRepository
import domain.model.endpoint.EndpointRepository.EndpointRepository
import domain.model.stream.StreamRepository

import zio.{Chunk, Has, ZLayer}
import zio.nio.channels.DatagramChannel
import zio.nio.core.{Buffer, SocketAddress}
import zio.stream.ZStream

object StreamRepositoryInMemory extends StreamRepository.Service {

  def fetchDatagram(server: DatagramChannel) =
    ZStream.repeatEffect {
      (for {
        size   <- ConfigRepository.getBufferSize
        buffer <- Buffer.byte(size.value)
        origin <- server.receive(buffer)
        _      <- buffer.flip
        chunk  <- buffer.getChunk()
      } yield (origin, chunk)).refineToOrDie[IOException]
    }

  override lazy val getDatagramStream: ZStream[EndpointRepository with ConfigRepository, IOException, (Option[SocketAddress], Chunk[Byte])] =
    ZStream.managed(EndpointRepository.getDatagramEndpoint).flatMap(fetchDatagram)

  def live: ZLayer[EndpointRepository, IOException, Has[StreamRepository.Service]] = ZLayer.succeed(this)
}