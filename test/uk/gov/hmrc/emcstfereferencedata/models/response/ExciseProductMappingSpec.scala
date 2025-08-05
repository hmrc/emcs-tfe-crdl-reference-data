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

class ExciseProductMappingSpec extends UnitSpec {

  val wineExciseProductCode = ExciseProductMapping(
    code = "W200",
    description = "Still wine and still fermented beverages other than wine and beer",
    category = "W",
    categoryDescription = Some("Wine and fermented beverages other than wine and beer"),
    unitOfMeasureCode = Some(3)
  )

  val wineNoOptionalFields = ExciseProductMapping(
    code = "W200",
    description = "Still wine and still fermented beverages other than wine and beer",
    category = "W",
    categoryDescription = None,
    unitOfMeasureCode = None
  )

  "reads" should {
    "read JSON to a model" in {
      Json
        .obj(
          "code"        -> "W200",
          "description" -> "Still wine and still fermented beverages other than wine and beer",
          "category"    -> "W",
          "categoryDescription" -> "Wine and fermented beverages other than wine and beer",
          "unitOfMeasureCode"   -> 3
        )
        .as[ExciseProductMapping] shouldBe wineExciseProductCode
    }

    "read JSON to a model with optional fields missing" in {
      Json
        .obj(
          "code"        -> "W200",
          "description" -> "Still wine and still fermented beverages other than wine and beer",
          "category"    -> "W"
        )
        .as[ExciseProductMapping] shouldBe wineNoOptionalFields
    }
  }

  "writes" should {
    "write JSON to a model" in {
      Json.toJson(wineExciseProductCode) shouldBe Json.obj(
        "code"        -> "W200",
        "description" -> "Still wine and still fermented beverages other than wine and beer",
        "category"    -> "W",
        "categoryDescription" -> "Wine and fermented beverages other than wine and beer",
        "unitOfMeasureCode"   -> 3
      )
    }

    "write JSON with no optional fields to a model" in {
      Json.toJson(wineNoOptionalFields) shouldBe Json.obj(
        "code"        -> "W200",
        "description" -> "Still wine and still fermented beverages other than wine and beer",
        "category"    -> "W"
      )
    }
  }
}
