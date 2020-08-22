package domain.model.config

import domain.model.values._
import zio._

object ConfigRepository {

  type ConfigRepository = Has[ConfigRepository.Service]

  trait Service {
    val getPrimaryUDPPort: IO[PortRangeException, Port]
    val getBufferSize: IO[BufferSizeException, BufferSize]
  }

  lazy val getPrimaryUDPPort: ZIO[ConfigRepository, PortRangeException, Port] =
    ZIO.accessM(_.get.getPrimaryUDPPort)

  lazy val getBufferSize: ZIO[ConfigRepository, BufferSizeException, BufferSize] =
    ZIO.accessM(_.get.getBufferSize)

}