package infrastructure.environment

import domain.model.chunkstream.ChunkStreamRepository._
import domain.model.config.ConfigRepository._
import domain.model.exception._
import infrastructure.persistance.endpoint._
import zio.console._
import zio._

object EndpointEnvironment {

  // TODO: NOT TOO PRETTY
  val fromChannel: ZLayer[ConfigRepository with Console, GatewayError, Channel] =
    ZLayer.fromAcquireRelease(EndpointFromSocket.datagramChannel)(channel => UIO(channel.close))
}