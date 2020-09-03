package domain.model.convertable

import zio.Has

object ConvertibleRepository {

  type ConvertibleRepository = Has[ConvertibleRepository.Service]

  trait Service {
    // def convert: ZIO[Any, Nothing, Int]
  }

}