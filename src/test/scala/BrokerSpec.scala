
import domain.model.broker.BrokerRepository
import infrastructure.environment.BrokerRepositoryEnvironment
import subgrpc.subscription.PublisherResponse
import zio.NonEmptyChunk
import zio.test._
import zio.test.Assertion._


object BrokerSpec extends DefaultRunnableSpec {

  type PR = PublisherResponse

  val testEnvironment = BrokerRepositoryEnvironment.fromSTM[PR]

  override def spec =
    suite("TransactionalBroker")(
      suite("topics")(
        testM("add subscriber") {
          for {
            _ <- BrokerRepository.addSubscriberTo[PR](NonEmptyChunk(NonEmptyChunk("root", "node", "leaf")), 1L)
            s <- BrokerRepository.getSubscribers[PR]("root/node/leaf").map(a => a.fold(-1)(_.size))
          } yield assert(s)(equalTo(1))
        },
        testM("add same subscriber twice") {
          for {
            _ <- BrokerRepository.addSubscriberTo[PR](NonEmptyChunk(NonEmptyChunk("root", "node", "leaf")), 1L).repeatN(2)
            s <- BrokerRepository.getSubscribers[PR]("root/node/leaf").map(a => a.fold(-1)(_.size))
          } yield assert(s)(equalTo(1))
        },
        testM("add two different subscriber to same topic") {
          for {
            _ <- BrokerRepository.addSubscriberTo[PR](NonEmptyChunk(NonEmptyChunk("root", "node", "leaf")), 1L)
            _ <- BrokerRepository.addSubscriberTo[PR](NonEmptyChunk(NonEmptyChunk("root", "node", "leaf")), 2L)
            s <- BrokerRepository.getSubscribers[PR]("root/node/leaf").map(a => a.fold(-1)(_.size))
          } yield assert(s)(equalTo(2))
        },
        testM("remove subscriber") {
          for {
            _ <- BrokerRepository.addSubscriberTo[PR](NonEmptyChunk(NonEmptyChunk("root", "node", "leaf")), 1L)
            _ <- BrokerRepository.removeSubscriber[PR](1L)
            s <- BrokerRepository.getSubscribers[PR]("root/node/leaf").map(a => a.fold(-1)(_.size))
          } yield assert(s)(equalTo(0))
        }
      ),
      suite("subscriber")(
        testM("remove subscriber") {
          for {
            _ <- BrokerRepository.addSubscriberTo[PR](NonEmptyChunk(NonEmptyChunk("root", "node", "leaf")), 1L)
            _ <- BrokerRepository.removeSubscriber[PR](1L)
            s <- BrokerRepository.getSubscribers[PR]("root/node/leaf").map(a => a.fold(-1)(_.size))
          } yield assert(s)(equalTo(0))
        },
        testM("add subscriber") {
          for {
            _ <- BrokerRepository.addSubscriberTo[PR](NonEmptyChunk(NonEmptyChunk("root", "node", "leaf")), 10L)
            s <- BrokerRepository.sizeSubscribers[PR]
          } yield assert(s)(equalTo(1))
        },
      ),
      suite("mailbox")(
        testM("remove mailbox") {
          for {
            _ <- BrokerRepository.addSubscriberTo[PR](NonEmptyChunk(NonEmptyChunk("root", "node", "leaf")), 1L)
            _ <- BrokerRepository.addSubscriberTo[PR](NonEmptyChunk(NonEmptyChunk("root", "node", "leaf")), 2L)
            _ <- BrokerRepository.removeSubscriber[PR](2L)
            s <- BrokerRepository.sizeMailboxes[PR]
          } yield assert(s)(equalTo(1))
        },
        testM("add mailbox") {
          for {
            _ <- BrokerRepository.addSubscriberTo[PR](NonEmptyChunk(NonEmptyChunk("root", "node", "leaf")), 1L)
            _ <- BrokerRepository.addSubscriberTo[PR](NonEmptyChunk(NonEmptyChunk("root", "node", "leaf")), 2L)
            s <- BrokerRepository.sizeMailboxes[PR]
          } yield assert(s)(equalTo(2))
        },
      ),
      suite("counter")(
        testM("increment") {
          for {
            a <- BrokerRepository.getNextId[PR]
            b <- BrokerRepository.getNextId[PR]
          } yield assert(a)(isLessThan(b))
        }
      )
    ).provideCustomLayer(testEnvironment)
}