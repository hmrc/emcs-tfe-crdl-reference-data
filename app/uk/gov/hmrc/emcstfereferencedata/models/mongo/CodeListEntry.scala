package uk.gov.hmrc.emcstfereferencedata.models.mongo

import play.api.libs.json.{JsObject, Json, OFormat}

case class CodeListEntry(
  codeListCode: CodeListCode,
  key: String,
  value: String,
  properties: JsObject
)

object CodeListEntry {
  given OFormat[CodeListEntry] = Json.format[CodeListEntry]
}