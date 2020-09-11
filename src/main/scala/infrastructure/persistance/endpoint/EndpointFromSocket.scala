package infrastructure.persistance.endpoint

import domain.model.config.ConfigRepository
import domain.model.config.ConfigRepository.ConfigRepository
import java.io.IOException

import domain.model.exception.{GatewayError, SystemError}
import zio._
import zio.nio.core.channels._
import zio.nio.core._

object EndpointFromSocket {

  val datagramChannel: ZIO[ConfigRepository, GatewayError, DatagramChannel] =
    (for {
      port          <- ConfigRepository.getPrimaryUDPPort
      socketAddress <- SocketAddress.inetSocketAddress(port.number).option
      server        <- DatagramChannel.open
      channel       <- server.bind(socketAddress)
    } yield channel).refineToOrDie[GatewayError]

}