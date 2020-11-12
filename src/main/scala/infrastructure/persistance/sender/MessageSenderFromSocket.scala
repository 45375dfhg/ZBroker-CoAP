package infrastructure.persistance.sender

import domain.model.chunkstream.ChunkStreamRepository.Channel
import domain.model.exception.GatewayError
import domain.model.sender.MessageSenderRepository
import zio._
import zio.console.Console
import zio.nio.core.{Buffer, SocketAddress}
import zio.nio.core.channels.DatagramChannel

object MessageSenderFromSocket extends MessageSenderRepository.Service {

  override def sendMessage(to: SocketAddress, msg: Chunk[Byte]): ZIO[Channel with Console, GatewayError, Unit] =
    (for {
      buffer  <- Buffer.byte(msg)
      server  <- ZIO.service[DatagramChannel]
      _       <- server.send(buffer, to)
    } yield ()).refineOrDieWith(PartialFunction.empty)(_ => new Throwable())
}