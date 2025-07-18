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

package uk.gov.hmrc.emcstfereferencedata.connector.retrieveProductCodes

import org.mockito.ArgumentMatchers.any
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.mockito.Mockito.when
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.emcstfereferencedata.fixtures.BaseFixtures
import uk.gov.hmrc.emcstfereferencedata.models.request.{CnInformationItem, CnInformationRequest}
import uk.gov.hmrc.emcstfereferencedata.models.response.{CnCodeInformation, ErrorResponse}
import uk.gov.hmrc.emcstfereferencedata.repositories.ExciseProductsRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class RetrieveProductCodesConnectorCRDLSpec
  extends AsyncWordSpec
  with Matchers
  with MockitoSugar
  with BaseFixtures {

  private val repository = mock[ExciseProductsRepository]
  private val connector  = new RetrieveProductCodesConnectorCRDL(repository)

  given HeaderCarrier = HeaderCarrier()

  "RetrieveProductCodesConnectorCRDL.retrieveProductCodes" when {
    "Given a CnInformationRequest" should {
      "Return a Map of Cncode to CnCodeInformation " in {
        when(repository.fetchProductCodesInformation(any()))
          .thenReturn(
            Future.successful(
              Map(testCnCode1 -> testCnCodeInformation1, testCnCode2 -> testCnCodeInformation2)
            )
          )

        connector
          .retrieveProductCodes(
            testCnCodeInformationRequest
          )
          .map(
            _ shouldBe Right(
              Map(testCnCode1 -> testCnCodeInformation1, testCnCode2 -> testCnCodeInformation2)
            )
          )
      }

    }
    "given an invalid excise code" should {
      "Return an empty Map" in {
        when(repository.fetchProductCodesInformation(any()))
          .thenReturn(Future.successful(Map.empty))

        connector
          .retrieveProductCodes(
            CnInformationRequest(List(CnInformationItem("M400", "invalid")))
          )
          .map(_ shouldBe Right(Map.empty))

      }
    }
    "return an Error Response" when {
      "there is a error fetching data" in {
        when(repository.fetchProductCodesInformation(any()))
          .thenReturn(Future.failed(new RuntimeException("Simulated failure")))

        connector
          .retrieveProductCodes(testCnCodeInformationRequest)
          .map(_ shouldBe Left(ErrorResponse.UnexpectedDownstreamResponseError))
      }
    }
  }

}
