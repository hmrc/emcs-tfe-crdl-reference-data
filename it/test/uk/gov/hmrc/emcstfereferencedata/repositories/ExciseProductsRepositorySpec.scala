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
import uk.gov.hmrc.emcstfereferencedata.models.response.{CnCodeInformation, ExciseProductCode}
import uk.gov.hmrc.mongo.test.DefaultPlayMongoRepositorySupport
import uk.gov.hmrc.mongo.transaction.{TransactionConfiguration, Transactions}
import play.api.test.Helpers.defaultAwaitTimeout

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

  private val testExciseProduct3 = ExciseProductCode(
    code = "B000",
    description = "Beer",
    category = "B",
    categoryDescription = "Beer",
    unitOfMeasureCode = 3
  )

  private val testExciseProduct4 = ExciseProductCode(
    code = "E200",
    description =
      "Vegetable and animal oils Products falling within CN codes 1507 to 1518, if these are intended for use as heating fuel or motor fuel (Article 20(1)(a))",
    category = "E",
    categoryDescription = "Energy Products",
    unitOfMeasureCode = 2
  )

  private val testExciseProduct5 = ExciseProductCode(
    code = "E300",
    description =
      "Mineral oils Products falling within CN codes 2707 10, 2707 20, 2707 30 and 2707 50 (Article 20(1)(b))",
    category = "E",
    categoryDescription = "Energy Products",
    unitOfMeasureCode = 2
  )

  private val testExciseProduct6 = ExciseProductCode(
    code = "W200",
    description = "Still wine and still fermented beverages other than wine and beer",
    category = "W",
    categoryDescription = "Wine and fermented beverages other than wine and beer",
    unitOfMeasureCode = 3
  )

  val exciseProductsListSorted: Seq[ExciseProductCode] = Seq(
    testExciseProduct3,
    testExciseProduct4,
    testExciseProduct5,
    testExciseProduct1,
    testExciseProduct2,
    testExciseProduct6
  )

  val exciseProductToCnCode1: CnCodeInformation =
    testCnCodeInformation1.copy(cnCodeDescription =
      testCnCodeInformation1.exciseProductCodeDescription
    )

  val exciseProductToCnCode2: CnCodeInformation =
    testCnCodeInformation2.copy(cnCodeDescription =
      testCnCodeInformation2.exciseProductCodeDescription
    )

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

  "ExciseProductsRepository.saveExciseProducts" should {
    "save new excise products" in {
      val exciseProducts = List(
        ExciseProductCode(
          "B000",
          "Beer",
          "B",
          "Beer",
          3
        ),
        ExciseProductCode(
          "E300",
          "Mineral oils Products falling within CN codes 2707 10, 2707 20, 2707 30 and 2707 50 (Article 20(1)(b))",
          "E",
          "Energy Products",
          2
        ),
        ExciseProductCode(
          "E460",
          "Kerosene, marked falling within CN code 2710 19 25 (Article 20(1)(c) of Directive 2003/96/EC)",
          "E",
          "Energy Products",
          2
        )
      )

      withSessionAndTransaction { repository.saveExciseProducts(_, exciseProducts) }.futureValue

      val insertedEntries = repository.collection.find().toFuture().futureValue

      insertedEntries should contain theSameElementsAs exciseProducts
    }

    "remove existing excise products when new excise products are saved" in {
      val existingProducts = List(
        ExciseProductCode(
          "B000",
          "Beer",
          "B",
          "Beer",
          3
        ),
        ExciseProductCode(
          "E300",
          "Mineral oils Products falling within CN codes 2707 10, 2707 20, 2707 30 and 2707 50 (Article 20(1)(b))",
          "E",
          "Energy Products",
          2
        ),
        ExciseProductCode(
          "E460",
          "Kerosene, marked falling within CN code 2710 19 25 (Article 20(1)(c) of Directive 2003/96/EC)",
          "E",
          "Energy Products",
          2
        )
      )

      repository.collection.insertMany(existingProducts).toFuture().futureValue

      val newProducts = List(
        ExciseProductCode(
          "S200",
          "Spirituous beverages",
          "S",
          "Ethyl alcohol and spirits",
          3
        ),
        ExciseProductCode(
          "E910",
          "Fatty-acid mono-alkyl esters, containing by weight 96,5 % or more of esters (FAMAE) falling within CN code 3826 00 10 (Article 20(1)(h) of Directive 2003/96/EC)",
          "E",
          "Energy Products",
          2
        ),
        ExciseProductCode(
          "T300",
          "Cigars & cigarillos",
          "T",
          "Manufactured tobacco products",
          4
        )
      )

      withSessionAndTransaction { repository.saveExciseProducts(_, newProducts) }.futureValue

      val insertedEntries = findAll().futureValue

      insertedEntries should contain theSameElementsAs newProducts
    }
  }

  "throw an error and keep existing products when no products are fetched from CRDL-cache" in {
    val existingProducts = List(
      ExciseProductCode(
        "B000",
        "Beer",
        "B",
        "Beer",
        3
      ),
      ExciseProductCode(
        "E300",
        "Mineral oils Products falling within CN codes 2707 10, 2707 20, 2707 30 and 2707 50 (Article 20(1)(b))",
        "E",
        "Energy Products",
        2
      ),
      ExciseProductCode(
        "E460",
        "Kerosene, marked falling within CN code 2710 19 25 (Article 20(1)(c) of Directive 2003/96/EC)",
        "E",
        "Energy Products",
        2
      )
    )

    repository.collection.insertMany(existingProducts).toFuture().futureValue

    val emptyList = List.empty
    val result = withSessionAndTransaction {
      repository.saveExciseProducts(_, emptyList)
    }

    assertThrows[MongoError.NoDataToInsert.type] {
      await(result)
    }

    val entries = findAll().futureValue

    entries should contain theSameElementsAs existingProducts
  }

  "ExciseProductsRepository.fetchAllEPCCodes" should {
    "return all excise products in the database" in {
      repository.collection.insertMany(exciseProductsListSorted).toFuture().futureValue

      repository
        .fetchAllEPCCodes()
        .futureValue should contain theSameElementsInOrderAs exciseProductsListSorted

    }
  }
  "ExciseProductsRepository.fetchProductCodesInformation" should {
    "return a Map of cnCodeInformation Items" when {
      "given the correct excise product codes" in {
        repository.collection.insertMany(exciseProductsListSorted).toFuture().futureValue

        val result =
          Map(testCnCode2 -> exciseProductToCnCode2, testCnCode1 -> exciseProductToCnCode1)

        repository
          .fetchProductCodesInformation(testCnCodeInformationRequest)
          .futureValue should contain theSameElementsAs result

      }
    }
  }
}
