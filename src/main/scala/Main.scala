
import infrastructure.persistance._

import zio._
import zio.console._
import zio.nio.core._
import zio.nio.InetAddress_


object Main extends App {

  def socketAddress = SocketAddress.inetSocketAddress("2001:0db8:85a3:0000:0000:8a2e:0370:7334", 8980)

  def run(args: List[String]): URIO[ZEnv, ExitCode] =
    (for {
      h <- socketAddress.flatMap(_.hostName)
      _ <- InetAddress.byName(h).map(new InetAddress_(_)) <*> InetAddress.byName(h).map(new InetAddress_(_)) >>=
        (t => putStrLn(t._1.hashCode().toString + " " + t._2.hashCode().toString + " " + t._1.equals(t._2).toString))
    } yield ()).orDie.exitCode

  //  val program = PublisherServer.make <&> SubscriberServer.make
//
//  def run(args: List[String]): URIO[ZEnv, ExitCode] = {
//    // val runtime = Runtime(ZEnv, Platform.default...) // TODO: custom error handling on runtime level
//
//    val env = Controller.boot(args)
//    program.provideCustomLayer(env).orDie.exitCode // TODO: .retryOrElse()
//  }

}

