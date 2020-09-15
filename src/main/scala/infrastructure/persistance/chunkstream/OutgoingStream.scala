package infrastructure.persistance.chunkstream

import domain.model.chunkstream.ChunkStreamRepository.Channel
import zio.clock.Clock
import zio.console._
import zio.duration._
import zio.{Schedule, ZIO}
import zio.nio.core.channels.DatagramChannel
import zio.nio.core.{Buffer, SocketAddress}
import zio.stream.ZStream

// TODO: Burn this down
object OutgoingStream {

  def send: ZStream[Console with Channel with Clock, Exception, Int] =
    ZStream.repeatEffectWith( {
      for {
        buffer  <- Buffer.byte(20)
        _       <- buffer.put(5)
        address <- SocketAddress.inetSocketAddress("127.0.0.1", 8080)
        server  <- ZIO.service[DatagramChannel]
        _       <- putStrLn("SEND")
        i       <- server.send(buffer, address)
      } yield i
    }, Schedule.spaced(100.second) && Schedule.recurs(100))
}
