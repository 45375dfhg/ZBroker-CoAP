package infrastructure.stream

import java.io.IOException

import domain.model.config.ConfigRepository
import domain.model.config.ConfigRepository.ConfigRepository
import domain.model.stream.ChunkStreamRepository
import domain.model.stream.ChunkStreamRepository.ChunkStreamRepository

import zio.{Chunk, Has, ZIO, ZLayer}
import zio.nio.core.channels.DatagramChannel
import zio.nio.core.{Buffer, SocketAddress}
import zio.stream.ZStream
import zio.console._


object ChunkStreamFromSocket extends ChunkStreamRepository.Service {

  override def getStream:
  ZStream[ConfigRepository with Has[DatagramChannel], IOException, (Option[SocketAddress], Chunk[Boolean])] =
    ZStream.repeatEffect {
      (for {
        size   <- ConfigRepository.getBufferSize
        buffer <- Buffer.byte(size.value)
        server <- ZIO.service[DatagramChannel]
        origin <- server.receive(buffer)
        _      <- buffer.flip
        chunk  <- buffer.getChunk()
        _      <- putStrLn(chunk.asBits.map(_.toString + ", ").mkString)
      } yield (origin, chunk.asBits)).refineToOrDie[IOException]
    }.provideSomeLayer[Has[DatagramChannel] with ConfigRepository](Console.live)

  val live: ZLayer[Any, IOException, ChunkStreamRepository] = ZLayer.succeed(this)
}

/*
def nioBlocking[A, C <: Channel](channel: C)(f: C => ZIO[Blocking, Exception, A]): ZIO[Blocking, Exception, A] =
    zio.blocking.blocking(f(channel)).fork.flatMap(_.join).onInterrupt(channel.close.ignore)

  def effect(channel: DatagramChannel) = (for {
    size   <- ConfigRepository.getBufferSize
    buffer <- Buffer.byte(size.value)
    origin <- channel.receive(buffer)
    _      <- buffer.flip
    chunk  <- buffer.getChunk()
  } yield (origin, chunk)).provideSomeLayer(ConfigRepositoryInMemory.live)

  def stream(channel: DatagramChannel) =
    ZStream.repeatEffect(effect(channel))

  val result: ZIO[Blocking with Has[DatagramChannel], Exception, Unit] =
    (for {
      channel <- ZIO.service[DatagramChannel]
      stream  <- nioBlocking(channel)(channel => stream(channel).take(1000000).runDrain)
    } yield (stream))

  def createEffect: DatagramChannel => ZIO[Blocking, Exception, (Option[SocketAddress], Chunk[Byte])] = channel =>
    (for {
      size   <- ConfigRepository.getBufferSize
      buffer <- Buffer.byte(size.value)
      origin <- channel.receive(buffer)
      _      <- buffer.flip
      chunk  <- buffer.getChunk()
    } yield (origin, chunk)).provideSomeLayer(ConfigRepositoryInMemory.live ++ Blocking.live)

  def blocking: ZIO[Has[DatagramChannel] with Blocking, IOException, (Option[SocketAddress], Chunk[Byte])] =
    (for {
      channel <- ZIO.service[DatagramChannel]
      result  <- nioBlocking(channel)(createEffect)
    } yield result).refineToOrDie[IOException]

  //override def getStream: ZStream[ConfigRepository with Has[DatagramChannel], IOException, (Any, Chunk[Byte])] =
  // ZStream.repeatEffect(blocking).provideSomeLayer[Has[DatagramChannel]](Blocking.live)

 */