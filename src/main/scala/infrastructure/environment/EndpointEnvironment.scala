package infrastructure.environment

import domain.model.config.ConfigRepository.ConfigRepository
import domain.model.exception.GatewayError
import infrastructure.persistance.endpoint.EndpointFromSocket

import zio.{Has, UIO, ZLayer}
import zio.nio.core.channels.DatagramChannel

object EndpointEnvironment {

  val fromChannel: ZLayer[ConfigRepository, GatewayError, Has[DatagramChannel]] =
    ZLayer.fromAcquireRelease(EndpointFromSocket.datagramChannel)(channel => UIO(channel.close))
}