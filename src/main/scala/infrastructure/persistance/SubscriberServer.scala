package infrastructure.persistance


import domain.api.SubscriptionService
import domain.model.broker.BrokerRepository._
import io.grpc.ServerBuilder
import scalapb.zio_grpc.CanBind.canBindAny
import scalapb.zio_grpc._
import subgrpc.subscription.PublisherResponse
import zio._
import zio.console.putStrLn

object SubscriberServer {

  def port = 8980 // TODO: Needs to be part of the config

  def subscriptionService = new SubscriptionService()

  def service: ServiceList[ZEnv with BrokerRepository[PublisherResponse]] = ServiceList.add(subscriptionService)

  def builder: ServerBuilder[_] = ServerBuilder.forPort(8980)

  def live: ZLayer[ZEnv with BrokerRepository[PublisherResponse], Throwable, Has[Server.Service]] = ServerLayer.fromServiceList(builder, service)

  val make = putStrLn("[SUB] SubscriberServer loading config. Starting ...") *> live.build.useForever
}
