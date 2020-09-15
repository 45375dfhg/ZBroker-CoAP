package infrastructure.environment

import domain.model.exception.GatewayError
import domain.model.sender.MessageSenderRepository.MessageSenderRepository
import infrastructure.persistance.sender.MessageSenderFromSocket
import zio.{Layer, ZLayer}

object MessageSenderRepositoryEnvironment {

  val fromSocket: Layer[GatewayError, MessageSenderRepository] = ZLayer.succeed(MessageSenderFromSocket)
}