import java.io.IOException

import PortRepository.PortRepository
import zio._
import zio.clock.Clock
import zio.{App, Schedule}
import zio.console._
import zio.nio.channels._
import zio.nio.core._
import zio.stream._
import zio.duration._
import zio.Layer

class Port private(val number: Int) extends AnyVal {}

object Port {

  // TODO: proper errors
  def apply(number: Int) =
    Either.cond(number >= 0 && number < 65535, new Port(number), Exception)
}

object PortRepository {

  type PortRepository = Has[PortRepository.Service]

  trait Service {
    def get(): IO[Exception, Port]
  }

  sealed trait ConfigFailure
  object ConfigFailure {
    final case class UnexpectedConfigFailure(err: Throwable) extends ConfigFailure
  }

  // TODO: Needs a proper "No Config Found" error etc.
  def get(): ZIO[PortRepository, Exception, Port] =
    ZIO.accessM(_.get.get())

}

sealed trait Endpoint
case object WebSocketEndpoint extends Endpoint
case object DatagramEndpoint extends Endpoint {

  private val channel: ZManaged[PortRepository, Exception, DatagramChannel] =
    for {
      port          <- PortRepository.get().toManaged_
      socketAddress <- SocketAddress.inetSocketAddress(port.number).option.toManaged_
      channel       <- DatagramChannel.bind(socketAddress)
    } yield channel

  val create = ZStream.managed(channel)

}

object PortRepositoryInMemory extends PortRepository.Service {

  override def get() = ZIO.fromEither(Port(7025))

  def toLayer: Layer[Nothing, Has[PortRepository.Service]] = ZLayer.succeed(this)
}

val program =
  (for {
    a <- DatagramEndpoint.create
  } yield ()).provideLayer(PortRepositoryInMemory.toLayer)


