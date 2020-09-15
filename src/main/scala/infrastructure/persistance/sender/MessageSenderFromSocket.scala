package infrastructure.persistance.sender

import java.io.IOException

import domain.model.chunkstream.ChunkStreamRepository.Channel
import domain.model.exception.{GatewayError, SystemError}
import domain.model.sender.MessageSenderRepository
import zio._
import zio.console.{Console, putStrLn}
import zio.nio.core.{Buffer, InetAddress, SocketAddress}
import zio.nio.core.channels.DatagramChannel

object MessageSenderFromSocket extends MessageSenderRepository.Service {

  override def sendMessage(to: SocketAddress, msg: Chunk[Byte]): ZIO[Channel with Console, GatewayError, Unit] =
    (for {
      buffer  <- Buffer.byte(msg)
      server  <- ZIO.service[DatagramChannel]
      _       <- server.send(buffer, to)
      _       <- putStrLn(to.toString + " -- " + msg.toString)
    } yield ()).refineOrDieWith(PartialFunction.empty)(_ => new Throwable("dead")) // TODO: proper refinement
}