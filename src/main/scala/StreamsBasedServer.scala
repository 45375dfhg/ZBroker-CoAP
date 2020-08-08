import zio._
import zio.duration._
import zio.nio.core.SocketAddress
import zio.nio.channels._
import zio.stream._
import zio.console._
import zio.clock._

object StreamsBasedServer extends App {

  def run(args: List[String]) =
    ZStream
      .managed(server(7080))
      .flatMap(handleConnections(_) { chunk =>
        console.putStrLn(s"Read data: ${chunk.mkString}") *>
          clock.sleep(2.seconds) *>
          console.putStrLn("Done")
      })
      .runDrain
      .orDie
      .exitCode

  def server(port: Int): Managed[Exception, AsynchronousServerSocketChannel] =
    for {
      server        <- AsynchronousServerSocketChannel()
      socketAddress <- SocketAddress.inetSocketAddress(port).toManaged_
      _             <- server.bind(socketAddress).toManaged_
    } yield server

    /* val server2 = AsynchronousServerSocketChannel()
  .mapM { socket =>
    for {
      _ <- SocketAddress.inetSocketAddress("127.0.0.1", 1337) >>= socket.bind
      _ <- socket.accept.preallocate.flatMap(_.use(channel => doWork(channel).catchAll(ex => putStrLn(ex.getMessage))).fork).forever.fork
    } yield ()
  }.useForever

  def doWork(channel: AsynchronousSocketChannel): ZIO[Console with Clock, Throwable, Unit] = {
  val process =
    for {
      chunk <- channel.read(3)
      str = chunk.toArray.map(_.toChar).mkString
      _ <- putStrLn(s"received: [$str] [${chunk.length}]")
    } yield ()

  process.whenM(channel.isOpen).forever
} */

  def handleConnections[R <: console.Console](
    server: AsynchronousServerSocketChannel
  )(f: String => RIO[R, Unit]): ZStream[R, Throwable, Unit] =
    ZStream
      .repeatEffect(server.accept.preallocate)
      .map(conn => ZStream.managed(conn.ensuring(console.putStrLn("Connection closed")).withEarlyRelease))
      .flatMapPar[R, Throwable, Unit](16) { connection =>
        connection
          .mapM {
            case (closeConn, channel) =>
              for {
                _    <- console.putStrLn("Received connection")
                data <- ZStream
                          .fromEffectOption(
                            channel.read(64).tap(_ => console.putStrLn("Read chunk")).orElse(ZIO.fail(None))
                          )
                          .flattenChunks
                          .take(4)
                          .transduce(ZTransducer.utf8Decode)
                          .run(Sink.foldLeft("")(_ + (_: String)))
                _    <- closeConn
                _    <- f(data)
              } yield ()
          }
      }
}