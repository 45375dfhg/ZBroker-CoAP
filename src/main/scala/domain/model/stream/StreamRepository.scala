package domain.model.stream

import java.io.IOException

import domain.model.config.ConfigRepository.ConfigRepository
import domain.model.endpoint.EndpointRepository.EndpointRepository

import zio.{Chunk, Has}
import zio.nio.core.SocketAddress
import zio.stream.ZStream

object StreamRepository {

  type StreamRepository = Has[StreamRepository.Service] with EndpointRepository with ConfigRepository

  trait Service {
    val getDatagramStream: ZStream[EndpointRepository with ConfigRepository, IOException, (Option[SocketAddress], Chunk[Byte])]
  }

  lazy val getDatagramStream: ZStream[StreamRepository, IOException, (Option[SocketAddress], Chunk[Byte])] =
    ZStream.accessStream(_.get[StreamRepository.Service].getDatagramStream)
}