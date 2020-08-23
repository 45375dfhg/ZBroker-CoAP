package infrastructure.endpoint

import domain.model.config.ConfigRepository
import domain.model.config.ConfigRepository.ConfigRepository
import java.io.IOException

import zio._
import zio.nio.channels._
import zio.nio.core._

object EndpointRepositoryFromSocket {

  private val datagramChannel: ZManaged[ConfigRepository, IOException, DatagramChannel] =
    for {
      port          <- ConfigRepository.getPrimaryUDPPort.toManaged_
      socketAddress <- SocketAddress.inetSocketAddress(port.number).option.toManaged_
      channel       <- DatagramChannel.bind(socketAddress)
    } yield channel

  val live = ZLayer.fromManaged(datagramChannel)

}