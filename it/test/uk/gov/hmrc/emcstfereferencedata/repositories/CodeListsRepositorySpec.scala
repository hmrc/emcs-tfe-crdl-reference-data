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

package uk.gov.hmrc.emcstfereferencedata.repositories

import org.mongodb.scala.*
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import play.api.libs.json.Json
import uk.gov.hmrc.emcstfereferencedata.fixtures.BaseFixtures
import uk.gov.hmrc.emcstfereferencedata.models.crdl.CrdlCodeListEntry
import uk.gov.hmrc.emcstfereferencedata.models.mongo.{CodeListCode, CodeListEntry}
import uk.gov.hmrc.emcstfereferencedata.models.response.{CnCodeInformation, ExciseProductCode}
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.mongo.transaction.{TransactionConfiguration, Transactions}

import scala.concurrent.ExecutionContext

class CodeListsRepositorySpec
  extends AnyWordSpec
  with DefaultPlayMongoRepositorySupport[CodeListEntry]
  with Matchers
  with ScalaFutures
  with BaseFixtures
  with Transactions {

  given TransactionConfiguration = TransactionConfiguration.strict
  given ec: ExecutionContext     = ExecutionContext.global

  override protected val repository: CodeListsRepository =
    new CodeListsRepository(mongoComponent)

  "CodeListsRepository" should {
    "save entries from the countries codelist" in {
      val codeListCode = CodeListCode("BC08")

      val crdlEntries = countriesResult.map { case (key, value) =>
        CrdlCodeListEntry(key, value, Json.obj())
      }.toList

      withSessionAndTransaction {
        repository.saveCodeListEntries(_, codeListCode, crdlEntries)
      }.futureValue

      val expectedEntries = crdlEntries.map(CodeListEntry.fromCrdlEntry(codeListCode, _))
      val insertedEntries = repository.collection.find().toFuture().futureValue

      insertedEntries should contain theSameElementsAs expectedEntries
    }

    "remove previous entries when a new list of entries is saved" in {
      val codeListCode = CodeListCode("BC66")

      val existingEntries = List(
        CodeListEntry(
          codeListCode,
          "B",
          "Beer",
          Json.obj("actionIdentification" -> "1084")
        ),
        CodeListEntry(
          codeListCode,
          "E",
          "Energy Products",
          Json.obj("actionIdentification" -> "1085")
        )
      )

      repository.collection.insertMany(existingEntries).toFuture().futureValue

      val newCrdlEntries = List(
        CrdlCodeListEntry(
          "S",
          "Ethyl alcohol and spirits",
          Json.obj("actionIdentification" -> "1087")
        ),
        CrdlCodeListEntry(
          "T",
          "Manufactured tobacco products",
          Json.obj("actionIdentification" -> "1088")
        )
      )

      withSessionAndTransaction {
        repository.saveCodeListEntries(_, codeListCode, newCrdlEntries)
      }.futureValue

      val expectedEntries = newCrdlEntries.map(CodeListEntry.fromCrdlEntry(codeListCode, _))
      val insertedEntries = repository.collection.find().toFuture().futureValue

      insertedEntries should contain theSameElementsAs expectedEntries
    }

    "combine together entries of the CN codes and excise products codelists using the CN code <-> excise products correspondence" in {
      val productsCode                = CodeListCode("BC36")
      val cnCodesCode                 = CodeListCode("BC37")
      val cnCodesToExciseProductsCode = CodeListCode("E200")

      val codeListEntries = Seq(
        // CN Codes <-> Excise Products
        CodeListEntry(
          cnCodesToExciseProductsCode,
          "22060059",
          "B000",
          Json.obj("actionIdentification" -> "355")
        ),
        CodeListEntry(
          cnCodesToExciseProductsCode,
          "22060059",
          "I000",
          Json.obj("actionIdentification" -> "526")
        ),
        CodeListEntry(
          cnCodesToExciseProductsCode,
          "22060059",
          "S200",
          Json.obj("actionIdentification" -> "561")
        ),
        CodeListEntry(
          cnCodesToExciseProductsCode,
          "22060059",
          "W200",
          Json.obj("actionIdentification" -> "711")
        ),
        CodeListEntry(
          cnCodesToExciseProductsCode,
          "27101925",
          "E450",
          Json.obj("actionIdentification" -> "441")
        ),
        CodeListEntry(
          cnCodesToExciseProductsCode,
          "27101925",
          "E460",
          Json.obj("actionIdentification" -> "443")
        ),
        CodeListEntry(
          cnCodesToExciseProductsCode,
          "27101944",
          "E430",
          Json.obj("actionIdentification" -> "2412")
        ),
        CodeListEntry(
          cnCodesToExciseProductsCode,
          "27101944",
          "E440",
          Json.obj("actionIdentification" -> "2413")
        ),
        // CN Codes
        CodeListEntry(
          cnCodesCode,
          "22060059",
          "Other still fermented beverages in containers holding 2 litres or less",
          Json.obj("actionIdentification" -> "323")
        ),
        CodeListEntry(
          cnCodesCode,
          "27101925",
          "Other kerosene",
          Json.obj("actionIdentification" -> "153")
        ),
        CodeListEntry(
          cnCodesCode,
          "27101944",
          "Other heavy gas oils for other purposes with a sulphur content not exceeding 0,001% by weight.",
          Json.obj("actionIdentification" -> "351")
        ),
        // Excise Products
        CodeListEntry(
          productsCode,
          "B000",
          "Beer",
          Json.obj(
            "unitOfMeasureCode"                  -> "3",
            "degreePlatoApplicabilityFlag"       -> true,
            "actionIdentification"               -> "1090",
            "exciseProductsCategoryCode"         -> "B",
            "alcoholicStrengthApplicabilityFlag" -> true,
            "densityApplicabilityFlag"           -> false
          )
        ),
        CodeListEntry(
          productsCode,
          "E430",
          "Gasoil, unmarked falling within CN codes 2710 19 42, 2710 19 44, 2710 19 46, 2710 19 47, 2710 19 48, 2710 20 11, 2710 20 16 and 2710 20 19 (Article 20(1)(c) of Directive 2003/96/EC)",
          Json.obj(
            "unitOfMeasureCode"                  -> "2",
            "degreePlatoApplicabilityFlag"       -> false,
            "actionIdentification"               -> "1095",
            "exciseProductsCategoryCode"         -> "E",
            "alcoholicStrengthApplicabilityFlag" -> false,
            "densityApplicabilityFlag"           -> true
          )
        ),
        CodeListEntry(
          productsCode,
          "E440",
          "Gasoil, marked falling within CN codes 2710 19 42, 2710 19 44, 2710 19 46, 2710 19 47, 2710 19 48, 2710 20 11, 2710 20 16 and 2710 20 19 (Article 20(1)(c) of Directive 2003/96/EC)",
          Json.obj(
            "unitOfMeasureCode"                  -> "2",
            "degreePlatoApplicabilityFlag"       -> false,
            "actionIdentification"               -> "1096",
            "exciseProductsCategoryCode"         -> "E",
            "alcoholicStrengthApplicabilityFlag" -> false,
            "densityApplicabilityFlag"           -> true
          )
        ),
        CodeListEntry(
          productsCode,
          "E450",
          "Kerosene, falling within CN code 2710 19 21 and unmarked kerosene falling within CN code 2710 19 25 (Article 20(1)(c) of Directive 2003/96/EC)",
          Json.obj(
            "unitOfMeasureCode"                  -> "2",
            "degreePlatoApplicabilityFlag"       -> false,
            "actionIdentification"               -> "1097",
            "exciseProductsCategoryCode"         -> "E",
            "alcoholicStrengthApplicabilityFlag" -> false,
            "densityApplicabilityFlag"           -> true
          )
        ),
        CodeListEntry(
          productsCode,
          "E460",
          "Kerosene, marked falling within CN code 2710 19 25 (Article 20(1)(c) of Directive 2003/96/EC)",
          Json.obj(
            "unitOfMeasureCode"                  -> "2",
            "degreePlatoApplicabilityFlag"       -> false,
            "actionIdentification"               -> "1098",
            "exciseProductsCategoryCode"         -> "E",
            "alcoholicStrengthApplicabilityFlag" -> false,
            "densityApplicabilityFlag"           -> true
          )
        ),
        CodeListEntry(
          productsCode,
          "I000",
          "Intermediate products",
          Json.obj(
            "unitOfMeasureCode"                  -> "3",
            "degreePlatoApplicabilityFlag"       -> true,
            "actionIdentification"               -> "1109",
            "exciseProductsCategoryCode"         -> "I",
            "alcoholicStrengthApplicabilityFlag" -> true,
            "densityApplicabilityFlag"           -> false
          )
        ),
        CodeListEntry(
          productsCode,
          "S200",
          "Spirituous beverages",
          Json.obj(
            "unitOfMeasureCode"                  -> "3",
            "degreePlatoApplicabilityFlag"       -> false,
            "actionIdentification"               -> "1110",
            "exciseProductsCategoryCode"         -> "S",
            "alcoholicStrengthApplicabilityFlag" -> true,
            "densityApplicabilityFlag"           -> false
          )
        ),
        CodeListEntry(
          productsCode,
          "W200",
          "Still wine and still fermented beverages other than wine and beer",
          Json.obj(
            "unitOfMeasureCode"                  -> "3",
            "degreePlatoApplicabilityFlag"       -> false,
            "actionIdentification"               -> "1119",
            "exciseProductsCategoryCode"         -> "W",
            "alcoholicStrengthApplicabilityFlag" -> true,
            "densityApplicabilityFlag"           -> false
          )
        )
      )

      val expectedCnCodes = List(
        CnCodeInformation(
          "22060059",
          "Other still fermented beverages in containers holding 2 litres or less",
          "B000",
          "Beer",
          unitOfMeasureCode = 3
        ),
        CnCodeInformation(
          "22060059",
          "Other still fermented beverages in containers holding 2 litres or less",
          "I000",
          "Intermediate products",
          unitOfMeasureCode = 3
        ),
        CnCodeInformation(
          "22060059",
          "Other still fermented beverages in containers holding 2 litres or less",
          "S200",
          "Spirituous beverages",
          unitOfMeasureCode = 3
        ),
        CnCodeInformation(
          "22060059",
          "Other still fermented beverages in containers holding 2 litres or less",
          "W200",
          "Still wine and still fermented beverages other than wine and beer",
          unitOfMeasureCode = 3
        ),
        CnCodeInformation(
          "27101925",
          "Other kerosene",
          "E450",
          "Kerosene, falling within CN code 2710 19 21 and unmarked kerosene falling within CN code 2710 19 25 (Article 20(1)(c) of Directive 2003/96/EC)",
          unitOfMeasureCode = 2
        ),
        CnCodeInformation(
          "27101925",
          "Other kerosene",
          "E460",
          "Kerosene, marked falling within CN code 2710 19 25 (Article 20(1)(c) of Directive 2003/96/EC)",
          unitOfMeasureCode = 2
        ),
        CnCodeInformation(
          "27101944",
          "Other heavy gas oils for other purposes with a sulphur content not exceeding 0,001% by weight.",
          "E430",
          "Gasoil, unmarked falling within CN codes 2710 19 42, 2710 19 44, 2710 19 46, 2710 19 47, 2710 19 48, 2710 20 11, 2710 20 16 and 2710 20 19 (Article 20(1)(c) of Directive 2003/96/EC)",
          unitOfMeasureCode = 2
        ),
        CnCodeInformation(
          "27101944",
          "Other heavy gas oils for other purposes with a sulphur content not exceeding 0,001% by weight.",
          "E440",
          "Gasoil, marked falling within CN codes 2710 19 42, 2710 19 44, 2710 19 46, 2710 19 47, 2710 19 48, 2710 20 11, 2710 20 16 and 2710 20 19 (Article 20(1)(c) of Directive 2003/96/EC)",
          unitOfMeasureCode = 2
        )
      )

      repository.collection.insertMany(codeListEntries).toFuture().futureValue

      val cnCodes = withClientSession(repository.buildCnCodes).futureValue

      cnCodes should contain theSameElementsAs expectedCnCodes
    }

    "combine together entries of the excise products and product categories codelists" in {
      val productsCode   = CodeListCode("BC36")
      val categoriesCode = CodeListCode("BC66")

      val codeListEntries = Seq(
        CodeListEntry(
          categoriesCode,
          "B",
          "Beer",
          Json.obj("actionIdentification" -> "1084")
        ),
        CodeListEntry(
          categoriesCode,
          "E",
          "Energy Products",
          Json.obj("actionIdentification" -> "1085")
        ),
        CodeListEntry(
          productsCode,
          "B000",
          "Beer",
          Json.obj(
            "unitOfMeasureCode"                  -> "3",
            "degreePlatoApplicabilityFlag"       -> true,
            "actionIdentification"               -> "1090",
            "exciseProductsCategoryCode"         -> "B",
            "alcoholicStrengthApplicabilityFlag" -> true,
            "densityApplicabilityFlag"           -> false
          )
        ),
        CodeListEntry(
          productsCode,
          "E300",
          "Mineral oils Products falling within CN codes 2707 10, 2707 20, 2707 30 and 2707 50 (Article 20(1)(b))",
          Json.obj(
            "unitOfMeasureCode"                  -> "2",
            "degreePlatoApplicabilityFlag"       -> false,
            "actionIdentification"               -> "1092",
            "exciseProductsCategoryCode"         -> "E",
            "alcoholicStrengthApplicabilityFlag" -> false,
            "densityApplicabilityFlag"           -> true
          )
        ),
        CodeListEntry(
          productsCode,
          "E460",
          "Kerosene, marked falling within CN code 2710 19 25 (Article 20(1)(c) of Directive 2003/96/EC)",
          Json.obj(
            "unitOfMeasureCode"                  -> "2",
            "degreePlatoApplicabilityFlag"       -> false,
            "actionIdentification"               -> "1098",
            "exciseProductsCategoryCode"         -> "E",
            "alcoholicStrengthApplicabilityFlag" -> false,
            "densityApplicabilityFlag"           -> true
          )
        )
      )

      val expectedExciseProducts = List(
        ExciseProductCode(
          "B000",
          "Beer",
          "B",
          "Beer"
        ),
        ExciseProductCode(
          "E300",
          "Mineral oils Products falling within CN codes 2707 10, 2707 20, 2707 30 and 2707 50 (Article 20(1)(b))",
          "E",
          "Energy Products"
        ),
        ExciseProductCode(
          "E460",
          "Kerosene, marked falling within CN code 2710 19 25 (Article 20(1)(c) of Directive 2003/96/EC)",
          "E",
          "Energy Products"
        )
      )

      repository.collection.insertMany(codeListEntries).toFuture().futureValue

      val exciseProducts = withClientSession(repository.buildExciseProducts).futureValue

      exciseProducts should contain theSameElementsAs expectedExciseProducts
    }
  }
}
