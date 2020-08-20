package domain.model.unidirectional

import domain.model.protocol._
import domain.model.interface._

final case class Unidirectional (
    pipeline: Protocol => Protocol,
    source: Interface,
    sink: Interface
)