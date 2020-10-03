package domain.api

import domain.model.broker.BrokerRepository
import domain.model.broker.BrokerRepository.BrokerRepository

import io.grpc.Status

import subgrpc.subscription.SubscriptionRequest.Action
import subgrpc.subscription._

import zio.stream.{Stream, ZStream}
import zio.{NonEmptyChunk, ZEnv}

class SubscriptionService extends ZioSubscription.ZSubscriptionService[ZEnv with BrokerRepository, Any] {
  import SubscriptionService._

  override def subscribe(request: Stream[Status, SubscriptionRequest]): ZStream[ZEnv with BrokerRepository, Status, PublisherResponse] = {

    ZStream.fromEffect(BrokerRepository.getNextId).flatMap { id =>
      ZStream.unwrap(BrokerRepository.getQueue(id).bimap(_ => Status.INTERNAL, q => ZStream.fromTQueue(q).ensuring(BrokerRepository.removeSubscriber(id))))
        .drainFork {
          for {
            _ <- request
              .collect(nonEmptyPaths andThen nonEmptySegments andThen notUnrecognized) // TODO: Really just drop elements?
              // IF COLLECT RETURNS NOTHING => NO QUEUE => OTHER STREAM FAILS!
              .tap { case (action, paths) => action match {
                case Action.ADD    => BrokerRepository.addSubscriberTo(paths, id)
                case Action.REMOVE => BrokerRepository.removeSubscriptions(paths, id)
              }
              }
          } yield ()
        }
    }
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