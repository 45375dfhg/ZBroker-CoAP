package infrastructure.config

import domain.model.config._

import zio._

object ConfigRepositoryInMemory extends ConfigRepository.Service {

  // TODO: Configure a proper fallback
  override def getPrimaryUDPPort: IO[PortRangeException, Port] = ZIO.fromEither(Port(7021))

  override def getBufferSize: IO[BufferSizeException, BufferSize] = ZIO.fromEither(BufferSize(100))

  val live: Layer[Nothing, Has[ConfigRepository.Service]] = ZLayer.succeed(this)
}