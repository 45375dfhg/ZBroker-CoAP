package domain.model

import domain.model.unidirectional._

final case class Gateway (
    in:  Unidirectional, 
    out: Unidirectional
)