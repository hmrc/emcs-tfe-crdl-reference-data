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
import play.api.test.Helpers.await
import uk.gov.hmrc.emcstfereferencedata.fixtures.BaseFixtures
import uk.gov.hmrc.emcstfereferencedata.models.errors.MongoError
import uk.gov.hmrc.emcstfereferencedata.models.request.{CnInformationItem, CnInformationRequest}
import uk.gov.hmrc.emcstfereferencedata.models.response.CnCodeInformation
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.mongo.transaction.{TransactionConfiguration, Transactions}
import play.api.test.Helpers.defaultAwaitTimeout

import scala.concurrent.ExecutionContext

class CnCodesRepositorySpec
  extends AnyWordSpec
  with DefaultPlayMongoRepositorySupport[CnCodeInformation]
  with Matchers
  with ScalaFutures
  with BaseFixtures
  with Transactions {

  given TransactionConfiguration = TransactionConfiguration.strict

  given ec: ExecutionContext = ExecutionContext.global

  override protected val repository: CnCodesRepository =
    new CnCodesRepository(mongoComponent)

  private val testCnCodes = List(
    testCnCodeInformation1,
    testCnCodeInformation2,
    testCnCodeInformation3,
    testCnCodeInformation4,
    testCnCodeInformation5,
    testCnCodeInformation6
  )

  private val cnCodeInformationItem3 = CnInformationItem(productCode = "W200", cnCode = "22042223")

  private val cnCodeInformationRequest2 = CnInformationRequest(items =
    Seq(testCnCodeInformationItem1, testCnCodeInformationItem2, cnCodeInformationItem3)
  )

  "CnCodesRepository.fetchCnCodesForProduct" should {
    "return matching CN codes for a given excise product code" in {
      repository.collection.insertMany(testCnCodes).toFuture().futureValue

      repository
        .fetchCnCodesForProduct("T400")
        .futureValue should contain only testCnCodeInformation1

      repository
        .fetchCnCodesForProduct("S500")
        .futureValue should contain only testCnCodeInformation2

      repository
        .fetchCnCodesForProduct("E430")
        .futureValue should contain theSameElementsAs List(
        testCnCodeInformation3,
        testCnCodeInformation5
      )

      repository
        .fetchCnCodesForProduct("E440")
        .futureValue should contain theSameElementsAs List(
        testCnCodeInformation4,
        testCnCodeInformation6
      )
    }
  }

  "CnCodesRepository.deleteCnCodes" should {
    "delete all CN code information from the underlying collection" in {
      repository.collection.insertMany(testCnCodes).toFuture().futureValue

      withSessionAndTransaction(repository.deleteCnCodes).futureValue
      repository.fetchCnCodesForProduct("T400").futureValue shouldBe empty
      repository.fetchCnCodesForProduct("S500").futureValue shouldBe empty
    }
  }

  "CnCodesRepository.fetchCnCodeInformation" should {
    "return a Map of CnCode to CnCodeInformation that corresponds to the provided request" in {
      repository.collection.insertMany(testCnCodes).toFuture().futureValue

      repository
        .fetchCnCodeInformation(testCnCodeInformationRequest)
        .futureValue shouldBe Map(
        testCnCode1 -> testCnCodeInformation1,
        testCnCode2 -> testCnCodeInformation2
      )
    }
    "return no details for unknown cnCodes" in {
      repository.collection.insertMany(testCnCodes).toFuture().futureValue

      repository
        .fetchCnCodeInformation(cnCodeInformationRequest2)
        .futureValue shouldBe Map(
        testCnCode1 -> testCnCodeInformation1,
        testCnCode2 -> testCnCodeInformation2
      )
    }
  }

  "CnCodesRepository.saveCnCodes" should {
    "save new CN codes" in {
      val cnCodes = List(
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
      )

      withSessionAndTransaction { repository.saveCnCodes(_, cnCodes) }.futureValue

      val insertedEntries = repository.collection.find().toFuture().futureValue

      insertedEntries should contain theSameElementsAs cnCodes
    }

    "remove existing CN codes when new CN codes are saved" in {
      val existingCnCodes = List(
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
      )

      repository.collection.insertMany(existingCnCodes).toFuture().futureValue

      val newCnCodes = List(
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

      withSessionAndTransaction { repository.saveCnCodes(_, newCnCodes) }.futureValue

      val insertedEntries = findAll().futureValue

      insertedEntries should contain theSameElementsAs newCnCodes
    }
    "throw an error and keep existing CnCodes when no products are fetched from CRDL-cache" in {
      val existingCnCodes = List(
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
      )


      repository.collection.insertMany(existingCnCodes).toFuture().futureValue

      val emptyList = List.empty
      val result = withSessionAndTransaction {
        repository.saveCnCodes(_, emptyList)
      }

      assertThrows[MongoError] {
        await(result)
      }

      val entries = findAll().futureValue

      entries should contain theSameElementsAs existingCnCodes
    }

  }
}
