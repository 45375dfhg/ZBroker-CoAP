
import domain.model.broker.BrokerRepository
import domain.model.coap.header.fields.CoapId
import infrastructure.environment.{BrokerRepositoryEnvironment, DuplicationTrackerRepositoryEnvironment}
import infrastructure.persistance.broker.TransactionalBroker
import infrastructure.persistance.dupetracker.DuplicationTracker
import subgrpc.subscription.PublisherResponse
import zio.NonEmptyChunk
import zio.nio.core.SocketAddress
import zio.test._
import zio.test.Assertion._


object DupeSpec extends DefaultRunnableSpec {

  type ID = (SocketAddress, CoapId)

  val testEnvironment = DuplicationTrackerRepositoryEnvironment.fromSTM[ID]

  override def spec =
    suite("DuplicationRejection")(
      suite("DuplicationTracker")(

      ),
      suite("DuplicateRejectionService")(

      )
    ).provideCustomLayer(testEnvironment)
}