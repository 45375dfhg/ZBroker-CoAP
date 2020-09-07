package infrastructure.endpoint

import domain.model.config.ConfigRepository
import domain.model.config.ConfigRepository.ConfigRepository

import java.io.IOException

import zio._
import zio.nio.core.channels._
import zio.nio.core._

object EndpointRepositoryFromSocket {

  private val datagramChannel: ZIO[ConfigRepository, IOException, DatagramChannel] =
    (for {
      port          <- ConfigRepository.getPrimaryUDPPort
      socketAddress <- SocketAddress.inetSocketAddress(port.number).option
      server        <- DatagramChannel.open
      channel       <- server.bind(socketAddress)
    } yield channel).refineToOrDie[IOException]

  val live: ZLayer[ConfigRepository, IOException, Has[DatagramChannel]] =
    ZLayer.fromAcquireRelease(datagramChannel)(channel => UIO(channel.close))
}