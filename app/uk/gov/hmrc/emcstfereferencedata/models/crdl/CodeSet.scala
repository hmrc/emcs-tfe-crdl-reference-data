/*
 * Copyright 2026 HM Revenue & Customs
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

package uk.gov.hmrc.emcstfereferencedata.models.crdl

import uk.gov.hmrc.emcstfereferencedata.models.crdl.CodeListCode.{BC36, BC37, BC66, E200, HMRCBC36, HMRCBC37, HMRCBC66, HMRCE200}

case class CodeSet(
  source: String,
  exciseProducts: CodeListCode,
  cnCodes: CodeListCode,
  productCategories: CodeListCode,
  cnCodeExciseProductCorrespondence: CodeListCode
)

object CodeSet {
  val eu = CodeSet(
    source = "EU",
    exciseProducts = BC36,
    cnCodes = BC37,
    productCategories = BC66,
    cnCodeExciseProductCorrespondence = E200
  )

  val uk = CodeSet(
    source = "UK",
    exciseProducts = HMRCBC36,
    cnCodes = HMRCBC37,
    productCategories = HMRCBC66,
    cnCodeExciseProductCorrespondence = HMRCE200
  )

  val values: Seq[CodeSet] = Seq(eu, uk)
}
