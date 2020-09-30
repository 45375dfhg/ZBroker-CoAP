

import domain.model.broker.BrokerRepository
import domain.model.broker.BrokerRepository.BrokerRepository
import infrastructure.environment.BrokerRepositoryEnvironment
import io.grpc.protobuf.services.ProtoReflectionService
import io.grpc.{ServerBuilder, Status}
import scalapb.zio_grpc.CanBind.canBindAny
import scalapb.zio_grpc.{Server, ServerLayer, ServerMain, ServiceList}
import subgrpc.subscription.SubscriptionRequest.Action
import subgrpc.subscription._
import zio.console.putStrLn
import zio.stream.{Stream, ZStream}
import zio.{Has, NonEmptyChunk, Schedule, ZEnv, ZLayer}

import scala.concurrent.duration.Duration


class SubscriptionService extends ZioSubscription.ZSubscriptionService[ZEnv with BrokerRepository, Any] {
  import SubscriptionService._

  override def subscribe(request: Stream[Status, SubscriptionRequest]): ZStream[ZEnv with BrokerRepository, Status, PublisherResponse] = {

    ZStream.fromEffect(BrokerRepository.getNextId).flatMap { id =>
      ZStream.unwrap(BrokerRepository.getQueue(id).bimap(_ => Status.INTERNAL.withDescription(id.toString), q => ZStream.fromTQueue(q)))
        .zipLeft {
          for {
            _ <- request
              .collect(nonEmptyPaths andThen nonEmptySegments andThen notUnrecognized) // TODO: Really just drop elements?
              // IF COLLECT RETURNS NOTHING => NO QUEUE => OTHER STREAM FAILS!
              .tap { case (action, paths) => action match {
                case Action.ADD    => BrokerRepository.addSubscriberTo(paths, id)
                case Action.REMOVE => BrokerRepository.removeSubscriber(paths, id)
              }
              }
          } yield ()
        }
    }

//      for {
//        _ <- request
//          .collect(nonEmptyPaths andThen nonEmptySegments andThen notUnrecognized) // TODO: Really just drop elements?
//          // IF COLLECT RETURNS NOTHING => NO QUEUE => OTHER STREAM FAILS!
//          .tap { case (action, paths) => action match {
//            case Action.ADD => putStrLn("fuck") *> BrokerRepository.addSubscriberTo(paths, id)
//            case Action.REMOVE => BrokerRepository.removeSubscriber(paths, id)
//          }
//          }
//      } yield ()
//    }

//      request
//        .collect(nonEmptyPaths andThen nonEmptySegments andThen notUnrecognized) // TODO: Really just drop elements?
//        // IF COLLECT RETURNS NOTHING => NO QUEUE => OTHER STREAM FAILS!
//        .tap { case (action, paths) => action match {
//            case Action.ADD    => putStrLn("fuck") *> BrokerRepository.addSubscriberTo(paths, id)
//            case Action.REMOVE => BrokerRepository.removeSubscriber(paths, id)
//          }
//        }
//        .zipRight(ZStream.unwrap(BrokerRepository.getQueue(id).bimap(_ => Status.INTERNAL.withDescription(id.toString), ZStream.fromTQueue)))
//    } // ZStream.fromIterable(Seq(PublisherResponse(None))) //.repeatElements(Schedule.forever)
//
  }

}

object SubscriptionService {

  val notUnrecognized: ActionPartial = {
    case t @ (a, _) if !a.isUnrecognized => t
  }

  val nonEmptySegments: SegmentPartial = {
    case (a, paths) if paths.forall(_.segments.nonEmpty) => (a, paths.map(_.segments match {
      case head +: tail => NonEmptyChunk.fromIterable(head, tail)
    }))
  }

  val nonEmptyPaths: PartialFunction[SubscriptionRequest, (SubscriptionRequest.Action, NonEmptyChunk[Path])] = {
    case SubscriptionRequest(action, head +: tail, _) => (action, NonEmptyChunk.fromIterable(head, tail))
  }

  type ActionPartial =
    PartialFunction[(SubscriptionRequest.Action, NonEmptyChunk[NonEmptyChunk[String]]), (SubscriptionRequest.Action, NonEmptyChunk[NonEmptyChunk[String]])]

  type SegmentPartial =
    PartialFunction[(SubscriptionRequest.Action, NonEmptyChunk[Path]), (SubscriptionRequest.Action, NonEmptyChunk[NonEmptyChunk[String]])]

  type PathPartial =
    PartialFunction[(SubscriptionRequest.Action, NonEmptyChunk[Path]), (SubscriptionRequest.Action, NonEmptyChunk[Path])]
}

object SubscriptionServer extends ServerMain {

  override def port = 8980

  val subscriptionService = new SubscriptionService()

  def service: ServiceList[ZEnv with BrokerRepository] = ServiceList.add(subscriptionService)

  override def builder = super.builder
  
  def live: ZLayer[ZEnv with BrokerRepository, Throwable, Has[Server.Service]] = ServerLayer.fromServiceList(builder, service)

  val logic = welcome *> live.build.useForever
  
  override def services = ???
}
