package infrastructure.endpoint

import domain.model.endpoint.EndpointRepository
import domain.model.config.ConfigRepository
import domain.model.config.ConfigRepository.ConfigRepository
import java.io.IOException


import zio._
import zio.nio.channels._
import zio.nio.core._

object EndpointRepositoryInMemory extends EndpointRepository.Service {

  private lazy val datagramChannel: ZManaged[ConfigRepository, IOException, DatagramChannel] =
    for {
      port          <- ConfigRepository.getPrimaryUDPPort.toManaged_
      socketAddress <- SocketAddress.inetSocketAddress(port.number).option.toManaged_
      channel       <- DatagramChannel.bind(socketAddress)
    } yield channel

  override lazy val getDatagramEndpoint: ZManaged[ConfigRepository, IOException, DatagramChannel] = datagramChannel

  def live: ZLayer[ConfigRepository, IOException, Has[EndpointRepository.Service]] =
    ZLayer.succeed(this)

}