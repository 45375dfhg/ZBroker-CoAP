package domain.model.sender

import java.io.IOException

import domain.model.chunkstream.ChunkStreamRepository.Channel
import domain.model.coap.CoapMessage
import domain.model.config.ConfigRepository.ConfigRepository
import domain.model.exception.GatewayError

import zio.ZIO
import zio.nio.core.SocketAddress


object CoapSenderRepository {

  type CoapSenderRepository = CoapSenderRepository.Service

  trait Service {
    def sendMessage(to: SocketAddress, msg: CoapMessage): ZIO[Channel with ConfigRepository, GatewayError, Unit]
  }

  def sendMessage(to: SocketAddress, msg: CoapMessage): ZIO[CoapSenderRepository with ConfigRepository with Channel, GatewayError, Unit] =
    ZIO.accessM(_.sendMessage(to, msg))
}

