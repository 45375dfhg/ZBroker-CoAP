package domain.model

import io.estatico.newtype.macros.newtype


package object RouteModel {

  @newtype case class Route(asString: String)

}