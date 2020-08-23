package domain.model.endpoint

import java.io.IOException

import zio.{Has, ZIO, ZLayer, ZManaged}
import zio.nio.channels.DatagramChannel
import domain.model.config.ConfigRepository.ConfigRepository
import domain.model.stream.StreamRepository
import domain.model.stream.StreamRepository.StreamRepository

object EndpointRepository {

  type EndpointRepository = Has[EndpointRepository.Service]

  trait Service {}


}