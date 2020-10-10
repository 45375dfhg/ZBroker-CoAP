package utility.typeClass

import domain.model.coap.body.fields._
import domain.model.coap.header.fields._


trait Extractor[A] {
  def extract(param: A): Int
}

object Extractor {

  def extract[A: Extractor](param: A)(implicit ex: Extractor[A]): Int =
    ex.extract(param)

  implicit class ExtractOps[A: Extractor](param: A) {
    def extract: Int = Extractor[A].extract(param)
  }

  def apply[A](implicit ex: Extractor[A]): Extractor[A] = ex

  implicit val extractVersion: Extractor[CoapVersion] =
    (param: CoapVersion) => param.value

  implicit val extractType: Extractor[CoapType] =
    (param: CoapType) => param.value

  implicit val extractTokenLength: Extractor[CoapTokenLength] =
    (param: CoapTokenLength) => param.value

  implicit val extractCodePrefix: Extractor[CoapCodePrefix] =
    (param: CoapCodePrefix) => param.value

  implicit val extractCodeSuffix: Extractor[CoapCodeSuffix] =
    (param: CoapCodeSuffix) => param.value

  implicit val extractCodeId: Extractor[CoapId] =
    (param: CoapId) => param.value

  implicit val extractCoapOptionDelta: Extractor[CoapOptionDelta] =
    (param: CoapOptionDelta) => param.value

  implicit val extractCoapExtendedDelta: Extractor[CoapOptionExtendedDelta] =
    (param: CoapOptionExtendedDelta) => param.value

  implicit val extractCoapOptionLength: Extractor[CoapOptionLength] =
    (param: CoapOptionLength) => param.value

  implicit val extractCoapExtendedLength: Extractor[CoapOptionExtendedLength] =
    (param: CoapOptionExtendedLength) => param.value
}