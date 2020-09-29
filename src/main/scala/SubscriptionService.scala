
import domain.model.broker.BrokerRepository
import domain.model.broker.BrokerRepository.BrokerRepository
import infrastructure.environment.BrokerRepositoryEnvironment
import io.grpc.protobuf.services.ProtoReflectionService
import io.grpc.{ServerBuilder, Status}
import scalapb.zio_grpc.CanBind.canBindAny
import scalapb.zio_grpc.{ServerLayer, ServerMain, ServiceList}
import subgrpc.subscription._
import zio.stream.{Stream, ZStream}
import zio.{ZEnv, ZIO, ZLayer, console}
import zio.console._

class SubscriptionService extends ZioSubscription.ZSubscriptionService[ZEnv with BrokerRepository, Any] {

  override def subscribe(request: Stream[Status, SubscriptionRequest]): ZStream[ZEnv with BrokerRepository, Status, PublisherResponse] = {

    request.flatMap { request =>
      val init =
        for {
          id <- BrokerRepository.getId
          _  <- putStrLn("test")
          _  <- request.action.value match {
                  case 0 => putStrLn(request.action.value.toString + id.toString) *> BrokerRepository.addSubscriberTo(request.subscriptions, id)
                  case 1 => putStrLn(request.action.value.toString) *> BrokerRepository.removeSubscriber(request.subscriptions, id)
                }
          _  <- BrokerRepository.getQueue(id).bimap(_ => putStrLn("no queue found"), _ => putStrLn("lol"))
        } yield id

      ZStream.unwrap(init.flatMap(BrokerRepository.getQueue).map(ZStream.fromTQueue)).orElseFail(Status.INTERNAL.withDescription(request.toProtoString))
    }

//    val connectionId = BrokerRepository.getId
//
//    request.mapM { request =>
//        connectionId.flatMap { id =>
//          request.action match {
//            case _ if SubscriptionRequest.ACTION_FIELD_NUMBER == 0 => BrokerRepository.addSubscriberTo(request.subscriptions, id)
//            case _ if SubscriptionRequest.ACTION_FIELD_NUMBER == 1 => BrokerRepository.removeSubscriber(request.subscriptions, id)
//            case _ => BrokerRepository.addSubscriberTo(request.subscriptions, id)
//          }
//        }
//    }
//    ZStream.unwrap(
//      for {
//        id    <- connectionId
//        queue <- BrokerRepository.getQueue(id)
//      } yield ZStream.fromTQueue(queue)
//    ).orElseFail(Status.INTERNAL)
  }
}

object SubscriptionServer extends ServerMain {

  override def port = 8980

  val subscriptionService = new SubscriptionService()

  def serviceList: ServiceList[ZEnv] = ServiceList.add(subscriptionService).provideLayer(ZEnv.live ++ BrokerRepositoryEnvironment.fromSTM)

  override def services: ServiceList[ZEnv] = serviceList

//  override def port: Int = 8980
//
//  val subscriptionService = new SubscriptionService()
//
//  override def services: ServiceList[ZEnv] =
//    ServiceList.add(subscriptionService).provideLayer(BrokerRepositoryEnvironment.fromSTM)


}