import java.io.IOException

import domain.model.config.ConfigRepository
import domain.model.config.ConfigRepository.ConfigRepository
import infrastructure.config.ConfigRepositoryInMemory
import infrastructure.stream.StreamRepositoryFromSocket
import domain.model.stream.StreamRepository
import zio._
import zio.App
import zio.console._
import zio.nio.channels.DatagramChannel
import zio.nio.core.SocketAddress
import zio.stream._

object Application extends App {

  val program =
    (for {
      _         <- ZStream.fromEffect(putStrLn("booting up ..."))
      _         <- ZStream.mergeAll(2, 16)(StreamRepository.getDatagramStream, StreamRepository.sendDatagramStream)
    } yield ()).runDrain

  private def datagramChannel: ZManaged[ConfigRepository, IOException, DatagramChannel] =
    for {
      port          <- ConfigRepository.getPrimaryUDPPort.toManaged_
      socketAddress <- SocketAddress.inetSocketAddress(port.number).option.toManaged_
      channel       <- DatagramChannel.bind(socketAddress)
    } yield channel

  val HasChannel: ZLayer[ConfigRepository, IOException, Has[DatagramChannel]] = ZLayer.fromManaged(datagramChannel)

  val partialLayer =
    ConfigRepositoryInMemory.live >+> HasChannel >+> StreamRepositoryFromSocket.live

  def run(args: List[String]) =
    program.tap(l => putStrLn(l.toString)).provideCustomLayer(partialLayer).orDie.exitCode

}

