package utility.classExtension

import subgrpc.subscription.{Path, PublisherResponse}
import zio.NonEmptyChunk

/**
 * Extends the PublisherResponse object to implement custom constructors.
 */
object PublisherResponseExtension {

  /**
   * Construct a PublisherResponse based on the given route and content
   * @param route The route property of the PublisherResponse
   * @param content The new content that is mapped to the route
   * @return A freshly constructed Publisher Response
   */
  def fromPathWith(route: NonEmptyChunk[String], content: String) =
    PublisherResponse(Some(Path(route.toSeq)), content)

  implicit def fromExtension(response: PublisherResponse.type): PublisherResponseExtension.type =
    PublisherResponseExtension

}
