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
import uk.gov.hmrc.emcstfereferencedata.fixtures.BaseFixtures
import uk.gov.hmrc.emcstfereferencedata.models.response.CnCodeInformation
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.mongo.transaction.{TransactionConfiguration, Transactions}

import scala.concurrent.ExecutionContext

class CnCodesRepositorySpec
  extends AnyWordSpec
  with DefaultPlayMongoRepositorySupport[CnCodeInformation]
  with Matchers
  with ScalaFutures
  with BaseFixtures
  with Transactions {

  given TransactionConfiguration = TransactionConfiguration.strict
  given ec: ExecutionContext     = ExecutionContext.global

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
        .futureValue should contain only Map(
        testCnCode1 -> testCnCodeInformationItem1,
        testCnCode2 -> testCnCodeInformation2
      )
    }
  }
}
