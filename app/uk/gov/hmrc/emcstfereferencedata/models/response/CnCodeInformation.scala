/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.emcstfereferencedata.models.response

import play.api.libs.json.*
import uk.gov.hmrc.emcstfereferencedata.utils.StringUtils

case class CnCodeInformation(
  cnCode: String,
  cnCodeDescription: String,
  exciseProductCode: String,
  exciseProductCodeDescription: String,
  unitOfMeasureCode: Int
)

object CnCodeInformation {
  given Reads[CnCodeInformation] = Json.reads[CnCodeInformation]

  given OWrites[CnCodeInformation] = (o: CnCodeInformation) =>
    Json.obj(
      "cnCode"                       -> o.cnCode,
      "cnCodeDescription"            -> StringUtils.addSmartQuotes(o.cnCodeDescription),
      "exciseProductCode"            -> o.exciseProductCode,
      "exciseProductCodeDescription" -> StringUtils.addSmartQuotes(o.exciseProductCodeDescription),
      "unitOfMeasureCode"            -> o.unitOfMeasureCode
    )

  val mongoFormat: OFormat[CnCodeInformation] = Json.format[CnCodeInformation]
}
