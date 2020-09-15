package domain.model.sender

import domain.model.chunkstream.ChunkStreamRepository.Channel
import domain.model.config.ConfigRepository.ConfigRepository
import domain.model.exception.GatewayError

import zio.{Chunk, ZIO}
import zio.nio.core.SocketAddress


object MessageSenderRepository {

  type MessageSenderRepository = MessageSenderRepository.Service

  trait Service {
    def sendMessage(to: Option[SocketAddress], msg: Chunk[Byte]): ZIO[Channel, GatewayError, Unit]
  }

  def sendMessage(to: Option[SocketAddress], msg: Chunk[Byte]): ZIO[MessageSenderRepository with Channel, GatewayError, Unit] =
    ZIO.accessM(_.sendMessage(to, msg))
}

