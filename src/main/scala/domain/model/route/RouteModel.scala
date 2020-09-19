package domain.model

import domain.model.exception.GatewayError
import io.estatico.newtype.macros.newtype
import io.estatico.newtype.ops._
import zio.IO

package object RouteModel {

  @newtype class Route private(val value: String)

  object Route {
    def apply(value: String) = ???
  }

  case object EmptyRouteException extends GatewayError {
    override def msg = ???
  }
}