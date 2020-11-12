
import domain.api.DuplicateRejectionService
import domain.model.coap.header.fields.CoapId
import domain.model.dupetracker.DuplicationTrackerRepository
import domain.model.dupetracker.DuplicationTrackerRepository.DuplicationTrackerRepository
import infrastructure.environment.DuplicationTrackerRepositoryEnvironment
import infrastructure.persistance.dupetracker.DuplicationTracker
import zio.ZIO
import zio.test.{testM, _}
import zio.test.Assertion._
import zio.duration._
import zio.clock._
import zio.nio.core.{InetSocketAddress, SocketAddress}


object DupeSpec extends DefaultRunnableSpec {

  type ID = (InetSocketAddress, CoapId)

  val testEnvironment =
    DuplicationTrackerRepositoryEnvironment.fromSTM[Int] ++ Clock.live ++ DuplicationTrackerRepositoryEnvironment.fromSTM[ID]

  override def spec =
    suite("DuplicationRejection Int")(
      suite("DuplicationTracker")(
        testM("addIf once") {
          for {
            _ <- DuplicationTrackerRepository.addIf[Int](5)
            s <- DuplicationTrackerRepository.size[Int]
          } yield assert(s)(equalTo(1))
        },
        testM("addIf same element twice") {
          for {
            _ <- DuplicationTrackerRepository.addIf[Int](5)
            _ <- DuplicationTrackerRepository.addIf[Int](5)
            s <- DuplicationTrackerRepository.size[Int]
          } yield assert(s)(equalTo(1))
        },
        testM("addIf once bool") {
          for {
            b <- DuplicationTrackerRepository.addIf[Int](5)
          } yield assert(b)(equalTo(true))
        },
        testM("addIf twice bool") {
          for {
            _ <- DuplicationTrackerRepository.addIf[Int](5)
            b <- DuplicationTrackerRepository.addIf[Int](5)
          } yield assert(b)(equalTo(false))
        },
      ),
      suite("DuplicateRejectionService")(
        testM("temporaryAdd adds") {
          for {
            _ <- DuplicateRejectionService.temporaryAdd(5)
            s <- ZIO.accessM[DuplicationTrackerRepository[Int]](_.get.size)
          } yield assert(s)(equalTo(1))
        },
        testM("temporaryAdd not deleted (early check)") {
          for {
            _ <- DuplicateRejectionService.temporaryAdd(5, 5)
            _ <- DuplicateRejectionService.temporaryAdd(6, 5)
            s <- ZIO.accessM[DuplicationTrackerRepository[Int]](_.get.size).delay(4.seconds)
          } yield assert(s)(equalTo(2))
        },
        testM("temporaryAdd deleted (late check)") {
          for {
            _ <- DuplicateRejectionService.temporaryAdd(5, 5)
            _ <- DuplicateRejectionService.temporaryAdd(6, 5)
            s <- ZIO.accessM[DuplicationTrackerRepository[Int]](_.get.size).delay(6.seconds)
          } yield assert(s)(equalTo(0))
        }
      ),
      suite("DuplicationRejection ID")(
        suite("DuplicationTracker ID")(
          testM("addIf once") {
            for {
              i <- CoapId(15)
              s <- SocketAddress.inetSocketAddress(8080)
              _ <- DuplicationTrackerRepository.addIf[ID]((s,i))
              s <- DuplicationTrackerRepository.size[ID]
            } yield assert(s)(equalTo(1))
          },
          testM("addIf same element twice") {
            for {
              i <- CoapId(15)
              s <- SocketAddress.inetSocketAddress(8080)
              _ <- DuplicationTrackerRepository.addIf[ID]((s,i))
              _ <- DuplicationTrackerRepository.addIf[ID]((s,i))
              s <- DuplicationTrackerRepository.size[ID]
            } yield assert(s)(equalTo(1))
          },
          testM("addIf once bool") {
            for {
              i <- CoapId(15)
              s <- SocketAddress.inetSocketAddress(8080)
              b <- DuplicationTrackerRepository.addIf[ID]((s,i))
            } yield assert(b)(equalTo(true))
          },
          testM("addIf twice bool") {
            for {
              i <- CoapId(15)
              s <- SocketAddress.inetSocketAddress(8080)
              _ <- DuplicationTrackerRepository.addIf[ID]((s,i))
              b <- DuplicationTrackerRepository.addIf[ID]((s,i))
            } yield assert(b)(equalTo(false))
          },
        ),
        suite("DuplicateRejectionService ID")(
          testM("temporaryAdd adds") {
            for {
              i <- CoapId(15)
              s <- SocketAddress.inetSocketAddress(8080)
              _ <- DuplicateRejectionService.temporaryAdd((s, i))
              si <- ZIO.accessM[DuplicationTrackerRepository[ID]](_.get.size)
            } yield assert(si)(equalTo(1))
          },
          testM("temporaryAdd not deleted (early check) id") {
            for {
              i1 <- CoapId(15)
              i2 <- CoapId(16)
              s <- SocketAddress.inetSocketAddress(8080)
              _ <- DuplicateRejectionService.temporaryAdd((s, i1), 5)
              _ <- DuplicateRejectionService.temporaryAdd((s, i2), 5)
              si <- ZIO.accessM[DuplicationTrackerRepository[ID]](_.get.size).delay(4.seconds)
            } yield assert(si)(equalTo(2))
          },
          testM("temporaryAdd not deleted (early check) adr") {
            for {
              i <- CoapId(15)
              s1 <- SocketAddress.inetSocketAddress(8080)
              s2 <- SocketAddress.inetSocketAddress(8081)
              _ <- DuplicateRejectionService.temporaryAdd((s1, i), 5)
              _ <- DuplicateRejectionService.temporaryAdd((s2, i), 5)
              s <- ZIO.accessM[DuplicationTrackerRepository[ID]](_.get.size).delay(4.seconds)
            } yield assert(s)(equalTo(2))
          },
          testM("temporaryAdd deleted (late check) id") {
            for {
              i1 <- CoapId(15)
              i2 <- CoapId(16)
              s <- SocketAddress.inetSocketAddress(8080)
              _ <- DuplicateRejectionService.temporaryAdd((s, i1), 5)
              _ <- DuplicateRejectionService.temporaryAdd((s, i2), 5)
              si <- ZIO.accessM[DuplicationTrackerRepository[ID]](_.get.size).delay(6.seconds)
            } yield assert(si)(equalTo(0))
          },
          testM("temporaryAdd deleted (late check) adr") {
            for {
              i <- CoapId(15)
              s1 <- SocketAddress.inetSocketAddress(8080)
              s2 <- SocketAddress.inetSocketAddress(8081)
              _ <- DuplicateRejectionService.temporaryAdd((s1, i), 5)
              _ <- DuplicateRejectionService.temporaryAdd((s2, i), 5)
              s <- ZIO.accessM[DuplicationTrackerRepository[ID]](_.get.size).delay(6.seconds)
            } yield assert(s)(equalTo(0))
          }
        )
      )
    ).provideCustomLayer(testEnvironment)
}