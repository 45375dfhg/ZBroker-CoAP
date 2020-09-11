package infrastructure.environment

import domain.model.chunkstream.ChunkStreamRepository.ChunkStreamRepository
import domain.model.exception.GatewayError
import infrastructure.persistance.chunkstream.{ChunkStreamFromMemory, ChunkStreamFromSocket}

import zio.{Layer, ULayer, ZLayer}

object ChunkStreamRepositoryEnvironment {

  val fromMemory: ULayer[ChunkStreamRepository] = ZLayer.succeed(ChunkStreamFromMemory)

  val fromSocket: Layer[GatewayError, ChunkStreamRepository] = ZLayer.succeed(ChunkStreamFromSocket)
}