package domain.model.endpoint

import java.io.IOException

import zio.{Has, ZManaged}
import zio.nio.channels.DatagramChannel
import domain.model.config.ConfigRepository.ConfigRepository

object EndpointRepository {

  type EndpointRepository = Has[EndpointRepository.Service]

  trait Service {
    val getDatagramEndpoint: ZManaged[ConfigRepository, IOException, DatagramChannel]
  }

  lazy val getDatagramEndpoint: ZManaged[EndpointRepository with ConfigRepository, IOException, DatagramChannel] =
    ZManaged.accessManaged(_.get.getDatagramEndpoint)
}