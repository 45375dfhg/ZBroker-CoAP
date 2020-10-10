
import infrastructure.persistance._
import zio._

object Main extends App {

    val program = PublisherServer.make <&> SubscriberServer.make

    def run(args: List[String]): URIO[ZEnv, ExitCode] = {
    // val runtime = Runtime(ZEnv, Platform.default...) // TODO: custom error handling on runtime level
      val env = Controller.boot(args)
      program.provideCustomLayer(env).orDie.exitCode // TODO: .retryOrElse()
    }

}

