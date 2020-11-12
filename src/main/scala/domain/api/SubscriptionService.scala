package domain.api

import domain.model.broker.BrokerRepository
import domain.model.broker.BrokerRepository._

import subgrpc.subscription.SubscriptionRequest._
import subgrpc.subscription.ZioSubscription._
import subgrpc.subscription._

import io.grpc._

import zio.stream._
import zio._

class SubscriptionService extends ZSubscriptionService[ZEnv with BrokerRepository[PublisherResponse], Any] {
  import SubscriptionService._

  override def subscribe(request: Stream[Status, SubscriptionRequest]): ZStream[ZEnv with BrokerRepository[PublisherResponse], Status, PublisherResponse] = {
    type PR = PublisherResponse

    ZStream.fromEffect(BrokerRepository.getNextId[PR]).flatMap { id =>
      ZStream.unwrap(
        BrokerRepository.getQueue[PR](id).bimap(
          _ => Status.INTERNAL,
          q => ZStream.fromTQueue(q).ensuring(BrokerRepository.removeSubscriber[PR](id).ignore))
      ).drainFork {
          request.collect(nonEmptyPaths andThen nonEmptySegments andThen notUnrecognized).tap {
            case (action, paths) => action match {
              case Action.ADD    => BrokerRepository.addSubscriberTo(paths, id)
              case Action.REMOVE => BrokerRepository.removeSubscriptions(paths, id)
            }
          }
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

  val nonEmptyPaths: PathPartial = {
    case SubscriptionRequest(action, head +: tail, _) => (action, NonEmptyChunk.fromIterable(head, tail))
  }

  type ActionPartial =
    PartialFunction[(SubscriptionRequest.Action, NonEmptyChunk[NonEmptyChunk[String]]), (SubscriptionRequest.Action, NonEmptyChunk[NonEmptyChunk[String]])]

  type SegmentPartial =
    PartialFunction[(SubscriptionRequest.Action, NonEmptyChunk[Path]), (SubscriptionRequest.Action, NonEmptyChunk[NonEmptyChunk[String]])]

  type PathPartial =
    PartialFunction[SubscriptionRequest, (SubscriptionRequest.Action, NonEmptyChunk[Path])]
}