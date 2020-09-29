
import domain.model.broker.BrokerRepository
import domain.model.broker.BrokerRepository.BrokerRepository
import infrastructure.environment.BrokerRepositoryEnvironment
import io.grpc.protobuf.services.ProtoReflectionService
import io.grpc.{ServerBuilder, Status}
import scalapb.zio_grpc.CanBind.canBindAny
import scalapb.zio_grpc.{ServerLayer, ServerMain, ServiceList}
import subgrpc.subscription._
import zio.stream.{Stream, ZStream}
import zio.{ZEnv, ZLayer}

class SubscriptionService extends ZioSubscription.ZSubscriptionService[ZEnv with BrokerRepository, Any] {

  override def subscribe(request: Stream[Status, SubscriptionRequest]): ZStream[BrokerRepository, Status, PublisherResponse] = {

    val connectionId = BrokerRepository.getId

    request.mapM { request =>
        connectionId.flatMap { id =>
          request.action match {
            case _ if SubscriptionRequest.ACTION_FIELD_NUMBER == 0 => BrokerRepository.addSubscriberTo(request.subscriptions, id)
            case _ if SubscriptionRequest.ACTION_FIELD_NUMBER == 1 => BrokerRepository.removeSubscriber(request.subscriptions, id)
          }
        }
    }

    ZStream.unwrap(
      for {
        id    <- connectionId
        queue <- BrokerRepository.getQueue(id)
      } yield ZStream.fromTQueue(queue)
    ).orElseFail(Status.INTERNAL)
    //BrokerRepository.getId.flatMap(BrokerRepository.getQueue).map(queue => ZStream.fromTQueue(queue))
  }
}

object SubscriptionServer extends ServerMain {

//  override def port: Int = 8980
//
//  val subscriptionService = new SubscriptionService()
//
//  override def services: ServiceList[ZEnv] =
//    ServiceList.add(subscriptionService).provideLayer(BrokerRepositoryEnvironment.fromSTM)

}