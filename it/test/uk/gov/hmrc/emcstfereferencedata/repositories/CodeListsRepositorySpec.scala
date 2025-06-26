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
import uk.gov.hmrc.emcstfereferencedata.models.response.ExciseProductCode
import uk.gov.hmrc.mongo.MongoUtils
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

  private val productsRepository: ExciseProductsRepository =
    new ExciseProductsRepository(mongoComponent)

  override protected def ensureIndexes(): Seq[String] = {
    MongoUtils.ensureIndexes(repository.collection, indexes, replaceIndexes = false).futureValue

    MongoUtils
      .ensureIndexes(
        productsRepository.collection,
        productsRepository.indexes,
        replaceIndexes = false
      )
      .futureValue
  }

  override protected def ensureSchemas(): Unit = {
    MongoUtils.ensureSchema(mongoComponent, repository.collection, optSchema).futureValue
    MongoUtils.ensureSchema(mongoComponent, productsRepository.collection, productsRepository.optSchema).futureValue
  }

  override protected def prepareDatabase(): Unit = {
    productsRepository.initialised.futureValue
    super.prepareDatabase()
  }

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

      insertedEntries should contain allElementsOf expectedEntries
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

      insertedEntries should contain allElementsOf expectedEntries
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
