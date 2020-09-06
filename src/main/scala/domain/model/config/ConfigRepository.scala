package domain.model.config

import zio._

object ConfigRepository {

  type ConfigRepository = Has[ConfigRepository.Service]

  trait Service {
    def getPrimaryUDPPort: IO[PortRangeException, Port]
    def getBufferSize: IO[BufferSizeException, BufferSize]
  }

  def getPrimaryUDPPort: ZIO[ConfigRepository, PortRangeException, Port] =
    ZIO.accessM(_.get.getPrimaryUDPPort)

  def getBufferSize: ZIO[ConfigRepository, BufferSizeException, BufferSize] =
    ZIO.accessM(_.get.getBufferSize)

}