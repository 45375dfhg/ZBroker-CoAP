package infrastructure.persistance


import domain.api.SubscriptionService
import domain.model.broker.BrokerRepository.BrokerRepository

import scalapb.zio_grpc.CanBind.canBindAny
import scalapb.zio_grpc.{Server, ServerLayer, ServerMain, ServiceList}

import zio.{Has, ZEnv, ZLayer}

object SubscriberServer extends ServerMain {

  override def port = 8980

  def subscriptionService = new SubscriptionService()

  def service: ServiceList[ZEnv with BrokerRepository] = ServiceList.add(subscriptionService)

  override def builder = super.builder

  def live: ZLayer[ZEnv with BrokerRepository, Throwable, Has[Server.Service]] = ServerLayer.fromServiceList(builder, service)

  val make = welcome *> live.build.useForever

  override def services = ???
}