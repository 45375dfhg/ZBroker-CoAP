package utility

import subgrpc.subscription.{Path, PublisherResponse}
import zio.NonEmptyChunk

object PublisherResponseExtension {

  def from(route: NonEmptyChunk[String], content: String) =
    PublisherResponse(Some(Path(route.toSeq)), content)

  implicit def fromExtension(response: PublisherResponse.type): PublisherResponseExtension.type = PublisherResponseExtension

}
