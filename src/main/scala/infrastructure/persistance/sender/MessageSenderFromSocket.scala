package infrastructure.persistance.sender

import domain.model.chunkstream.ChunkStreamRepository.Channel
import domain.model.coap.CoapMessage
import domain.model.config.ConfigRepository
import domain.model.config.ConfigRepository.ConfigRepository
import domain.model.exception.SystemError
import domain.model.sender.MessageSenderRepository

import zio._
import zio.nio.core.{Buffer, SocketAddress}
import zio.nio.core.channels.DatagramChannel

object MessageSenderFromSocket extends MessageSenderRepository.Service {
  override def sendMessage(to: Option[SocketAddress], msg: Chunk[Byte]): ZIO[Channel, SystemError, Unit] =
    (for {
      buffer <- Buffer.byte(msg)
      server <- ZIO.service[DatagramChannel]

    } yield ()) // TODO: Precise the conversion
}