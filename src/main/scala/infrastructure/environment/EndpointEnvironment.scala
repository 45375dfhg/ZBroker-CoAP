package infrastructure.environment

import domain.model.chunkstream.ChunkStreamRepository.Channel
import domain.model.config.ConfigRepository.ConfigRepository
import domain.model.exception.GatewayError
import infrastructure.persistance.endpoint.EndpointFromSocket
import zio.{UIO, ZLayer}

object EndpointEnvironment {

  val fromChannel: ZLayer[ConfigRepository, GatewayError, Channel] =
    ZLayer.fromAcquireRelease(EndpointFromSocket.datagramChannel)(channel => UIO(channel.close))
}