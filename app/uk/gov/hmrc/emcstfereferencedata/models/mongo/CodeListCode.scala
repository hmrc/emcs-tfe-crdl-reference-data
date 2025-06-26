package uk.gov.hmrc.emcstfereferencedata.models.mongo

import play.api.libs.json.{Format, Json}

case class CodeListCode(value: String) extends AnyVal

object CodeListCode {
  given Format[CodeListCode] = Json.valueFormat[CodeListCode]
}
