package infrastructure.persistance.endpoint

import domain.model.config.ConfigRepository
import domain.model.config.ConfigRepository.ConfigRepository

import domain.model.exception.GatewayError
import zio._
import zio.nio.core.channels._
import zio.nio.core._

object EndpointFromSocket {

  val datagramChannel: ZIO[ConfigRepository, GatewayError, DatagramChannel] =
    (for {
      port          <- ConfigRepository.getPrimaryUDPPort
      socketAddress <- SocketAddress.inetSocketAddress(port.value).option
      server        <- DatagramChannel.open
      channel       <- server.bind(socketAddress)
    } yield channel).refineToOrDie[GatewayError]

}