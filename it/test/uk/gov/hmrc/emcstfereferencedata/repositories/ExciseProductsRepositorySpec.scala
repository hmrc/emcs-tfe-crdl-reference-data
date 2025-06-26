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
import uk.gov.hmrc.emcstfereferencedata.models.response.{CnCodeInformation, ExciseProductCode}
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.mongo.transaction.{TransactionConfiguration, Transactions}

import scala.concurrent.ExecutionContext

class ExciseProductsRepositorySpec
  extends AnyWordSpec
  with DefaultPlayMongoRepositorySupport[ExciseProductCode]
  with Matchers
  with ScalaFutures
  with BaseFixtures
  with Transactions {

  given TransactionConfiguration = TransactionConfiguration.strict
  given ec: ExecutionContext     = ExecutionContext.global

  override protected val repository: ExciseProductsRepository =
    new ExciseProductsRepository(mongoComponent)

  private val testProducts = List(testExciseProduct1, testExciseProduct2)

  "ExciseProductsRepository.fetchExciseProductsForCategory" should {
    "return matching excise products for a given excise product category code" in {
      repository.collection.insertMany(testProducts).toFuture().futureValue

      repository
        .fetchExciseProductsForCategory("S")
        .futureValue should contain only testExciseProduct1

      repository
        .fetchExciseProductsForCategory("T")
        .futureValue should contain only testExciseProduct2
    }
  }

  "ExciseProductsRepository.deleteExciseProducts" should {
    "delete all excise products from the underlying collection" in {
      repository.collection.insertMany(testProducts).toFuture().futureValue

      withSessionAndTransaction(repository.deleteExciseProducts).futureValue
      repository.fetchExciseProductsForCategory("S").futureValue shouldBe empty
      repository.fetchExciseProductsForCategory("T").futureValue shouldBe empty
    }
  }
}
