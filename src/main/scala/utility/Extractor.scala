package utility

import domain.model.coap._

trait Extractor[A] {
  def extract(param: A): Int
}

object Extractor {

  def extract[A: Extractor](param: A)(implicit ex: Extractor[A]) =
    ex.extract(param)

  implicit class ExtractOps[A: Extractor](param: A) {
    def extract: Int = Extractor[A].extract(param)
  }

  def apply[A](implicit ex: Extractor[A]): Extractor[A] = ex

  implicit val extractVersion: Extractor[CoapVersion] =
    (param: CoapVersion) => param.number

  implicit val extractType: Extractor[CoapType] =
    (param: CoapType) => param.number

  implicit val extractTokenLength: Extractor[CoapTokenLength] =
    (param: CoapTokenLength) => param.value

  implicit val extractCodePrefix: Extractor[CoapCodePrefix] =
    (param: CoapCodePrefix) => param.number

  implicit val extractCodeSuffix: Extractor[CoapCodeSuffix] =
    (param: CoapCodeSuffix) => param.number

  implicit val extractCodeId: Extractor[CoapId] =
    (param: CoapId) => param.value

  implicit val extractCoapOptionDelta: Extractor[CoapOptionDelta] =
    (param: CoapOptionDelta) => param.value

  implicit val extractCoapExtendedDelta: Extractor[CoapExtendedDelta] =
    (param: CoapExtendedDelta) => param.value

  implicit val extractCoapExtendedLength: Extractor[CoapExtendedLength] =
    (param: CoapExtendedLength) => param.value
}