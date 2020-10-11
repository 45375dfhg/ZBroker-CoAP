package infrastructure.persistance.endpoint

import domain.model.config.ConfigRepository
import domain.model.config.ConfigRepository.ConfigRepository
import domain.model.exception.GatewayError
import zio._
import zio.console._
import zio.nio.core.channels._
import zio.nio.core._

object EndpointFromSocket {

  val datagramChannel: ZIO[ConfigRepository with Console, GatewayError, DatagramChannel] =
    (for {
      port          <- ConfigRepository.getPrimaryUDPPort
      socketAddress <- SocketAddress.inetSocketAddress(port.value).optional
      server        <- DatagramChannel.open
      _             <- server.bind(socketAddress)
    } yield server).refineToOrDie[GatewayError]

}