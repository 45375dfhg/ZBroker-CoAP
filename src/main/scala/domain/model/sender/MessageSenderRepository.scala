package domain.model.sender

import domain.model.chunkstream.ChunkStreamRepository.Channel
import domain.model.exception.GatewayError
import zio.{Chunk, Has, ZIO}
import zio.nio.core.SocketAddress


object MessageSenderRepository {

  type MessageSenderRepository = Has[MessageSenderRepository.Service]

  trait Service {
    def sendMessage(to: SocketAddress, msg: Chunk[Byte]): ZIO[Channel, GatewayError, Unit]
  }

  def sendMessage(to: SocketAddress, msg: Chunk[Byte]): ZIO[MessageSenderRepository with Channel, GatewayError, Unit] =
    ZIO.accessM(_.get.sendMessage(to, msg))
}

