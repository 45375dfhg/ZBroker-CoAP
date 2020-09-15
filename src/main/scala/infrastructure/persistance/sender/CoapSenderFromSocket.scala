package infrastructure.persistance.sender

import domain.model.chunkstream.ChunkStreamRepository.Channel
import domain.model.coap.CoapMessage
import domain.model.config.ConfigRepository
import domain.model.config.ConfigRepository.ConfigRepository
import domain.model.exception.SystemError
import domain.model.sender.CoapSenderRepository

import zio._
import zio.nio.core.{Buffer, SocketAddress}
import zio.nio.core.channels.DatagramChannel

object CoapSenderFromSocket extends CoapSenderRepository.Service {
  override def sendMessage(to: SocketAddress, msg: CoapMessage): ZIO[ConfigRepository with Channel, SystemError, Unit] =
    (for {
      server <- ZIO.service[DatagramChannel]
      size   <- ConfigRepository.getOutwardBufferSize
      buffer <- Buffer.byte(size.value)

    } yield ()).refineToOrDie[SystemError] // TODO: Precise the conversion
}