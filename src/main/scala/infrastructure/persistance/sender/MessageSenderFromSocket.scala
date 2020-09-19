package infrastructure.persistance.sender

import java.io.IOException
import java.net.InetSocketAddress

import domain.model.chunkstream.ChunkStreamRepository.Channel
import domain.model.exception.NoResponseAvailable.NoResponseAvailable
import domain.model.exception.GatewayError
import domain.model.sender.MessageSenderRepository
import zio._
import zio.console.Console
import zio.nio.core.{Buffer, SocketAddress}
import zio.nio.core.channels.DatagramChannel

object MessageSenderFromSocket extends MessageSenderRepository.Service {

  override def sendMessage(to: SocketAddress, msg: Either[NoResponseAvailable, Chunk[Byte]]): ZIO[Channel with Console, GatewayError, Unit] =
    (for {
      data    <- IO.fromEither(msg)
      buffer  <- Buffer.byte(data)
      server  <- ZIO.service[DatagramChannel]
      _       <- server.send(buffer, to)
      //_       <- putStrLn(to.toString + " -- " + msg.toString)
    } yield ()).refineToOrDie[GatewayError]
}