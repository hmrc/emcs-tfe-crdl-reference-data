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

package uk.gov.hmrc.emcstfereferencedata.connector.retrieveAllEPCCodes

import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.emcstfereferencedata.fixtures.BaseFixtures
import uk.gov.hmrc.emcstfereferencedata.models.response.{ErrorResponse, ExciseProductCode}
import uk.gov.hmrc.emcstfereferencedata.repositories.ExciseProductsRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class RetrieveAllEPCCodesConnectorCRDLSpec
  extends AsyncWordSpec
  with Matchers
  with MockitoSugar
  with BaseFixtures {

  private val repository = mock[ExciseProductsRepository]
  private val connector  = new RetrieveAllEPCCodesConnectorCRDL(repository)

  given HeaderCarrier = HeaderCarrier()

  val exciseProductsList: Seq[ExciseProductCode] = Seq(
    ExciseProductCode(
      code = "W200",
      description = "Still wine and still fermented beverages other than wine and beer",
      category = "W",
      categoryDescription = "Wine and fermented beverages other than wine and beer",
      unitOfMeasureCode = 3
    ),
    ExciseProductCode(
      code = "B000",
      description = "Beer",
      category = "B",
      categoryDescription = "Beer",
      unitOfMeasureCode = 3
    ),
    ExciseProductCode(
      code = "E200",
      description =
        "Vegetable and animal oils Products falling within CN codes 1507 to 1518, if these are intended for use as heating fuel or motor fuel (Article 20(1)(a))",
      category = "E",
      categoryDescription = "Energy Products",
      unitOfMeasureCode = 2
    ),
    ExciseProductCode(
      code = "E300",
      description =
        "Mineral oils Products falling within CN codes 2707 10, 2707 20, 2707 30 and 2707 50 (Article 20(1)(b))",
      category = "E",
      categoryDescription = "Energy Products",
      unitOfMeasureCode = 2
    )
  )

  "RetrieveAllEPCCodesConnectorCRDL.retrieveAllEPCCodes" should {
    "Return a Sequence of all excise products" in {
      when(repository.fetchAllEPCCodes())
        .thenReturn(
          Future.successful(exciseProductsList)
        )

      connector
        .retrieveAllEPCCodes()
        .map(
          _ shouldBe Right(exciseProductsList)
        )
    }

    "Return an empty list" when {
      "there are no Excise items in the database" in {
        when(repository.fetchAllEPCCodes())
          .thenReturn(Future.successful(Seq.empty))

        connector
          .retrieveAllEPCCodes()
          .map(_ shouldBe Right(Seq.empty))

      }
    }
    "return an Error Response" when {
      "there is a error fetching data" in {
        when(repository.fetchAllEPCCodes())
          .thenReturn(Future.failed(new RuntimeException("Simulated failure")))

        connector
          .retrieveAllEPCCodes()
          .map(_ shouldBe Left(ErrorResponse.UnexpectedDownstreamResponseError))
      }
    }
  }

}
