/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.libs.json.Json
import uk.gov.hmrc.emcstfereferencedata.support.UnitSpec

class CnCodeMappingSpec extends UnitSpec {

  private val testCommodityCodeTobacco = CnCodeMapping(
    cnCode = "24029000",
    cnCodeDescription = Some("Cigarettes containing tobacco / other"),
    exciseProductCode = "T400",
    exciseProductCodeDescription = Some("Cigarettes"),
    unitOfMeasureCode = Some(1)
  )

  private val testNoOptionalFields = CnCodeMapping(
    cnCode = "24029000",
    cnCodeDescription = None,
    exciseProductCode = "T400",
    exciseProductCodeDescription = None,
    unitOfMeasureCode = None
  )

  "reads" should {
    "read JSON to a model" in {
      Json
        .obj(
          "cnCode"                       -> "24029000",
          "cnCodeDescription"            -> "Cigarettes containing tobacco / other",
          "exciseProductCode"            -> "T400",
          "exciseProductCodeDescription" -> "Cigarettes",
          "unitOfMeasureCode"            -> 1
        )
        .as[CnCodeMapping] shouldBe testCommodityCodeTobacco
    }

    "read JSON to a model with optional fields missing" in {
      Json
        .obj(
          "cnCode"            -> "24029000",
          "exciseProductCode" -> "T400"
        )
        .as[CnCodeMapping] shouldBe testNoOptionalFields
    }
  }

  "writes" should {
    "write JSON to a model" in {
      Json.toJson(testCommodityCodeTobacco) shouldBe Json.obj(
        "cnCode"                       -> "24029000",
        "cnCodeDescription"            -> "Cigarettes containing tobacco / other",
        "exciseProductCode"            -> "T400",
        "exciseProductCodeDescription" -> "Cigarettes",
        "unitOfMeasureCode"            -> 1
      )
    }

    "write JSON with no optional fields to a model" in {
      Json.toJson(testNoOptionalFields) shouldBe Json.obj(
        "cnCode"            -> "24029000",
        "exciseProductCode" -> "T400"
      )
    }
  }
}
