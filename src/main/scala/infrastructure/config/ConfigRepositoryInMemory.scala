package infrastructure.config

import domain.model.config.ConfigRepository
import domain.model.values._
import zio._

object ConfigRepositoryInMemory extends ConfigRepository.Service {

  // TODO: Configure a proper fallback
  override def getPrimaryUDPPort: IO[PortRangeException, Port] = ZIO.fromEither(Port(7025))

  override def getBufferSize: IO[BufferSizeException, BufferSize] = ZIO.fromEither(BufferSize(100))

  def live: Layer[Nothing, Has[ConfigRepository.Service]] = ZLayer.succeed(this)
}