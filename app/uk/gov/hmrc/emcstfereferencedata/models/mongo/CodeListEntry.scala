package uk.gov.hmrc.emcstfereferencedata.models.mongo

import play.api.libs.json.{JsObject, Json, OFormat}
import uk.gov.hmrc.emcstfereferencedata.models.crdl.CrdlCodeListEntry

case class CodeListEntry(
  codeListCode: CodeListCode,
  key: String,
  value: String,
  properties: JsObject
)

object CodeListEntry {
  given format: OFormat[CodeListEntry] = Json.format[CodeListEntry]

  def fromCrdlEntry(codeListCode: CodeListCode, entry: CrdlCodeListEntry): CodeListEntry =
    CodeListEntry(codeListCode, entry.key, entry.value, entry.properties)
}