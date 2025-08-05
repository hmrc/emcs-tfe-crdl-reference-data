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

package uk.gov.hmrc.emcstfereferencedata.connector

import org.mockito.ArgumentMatchers.any
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.mockito.Mockito.{reset, when}
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.emcstfereferencedata.fixtures.BaseFixtures
import uk.gov.hmrc.emcstfereferencedata.models.request.{CnInformationItem, CnInformationRequest}
import uk.gov.hmrc.emcstfereferencedata.models.response.{CnCodeInformation, ErrorResponse}
import uk.gov.hmrc.emcstfereferencedata.repositories.CnCodesRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future
import org.scalatest.BeforeAndAfterEach

class RetrieveCnCodeInformationConnectorSpec
  extends AsyncWordSpec
  with Matchers
  with MockitoSugar
  with BaseFixtures
  with BeforeAndAfterEach {

  private val repository               = mock[CnCodesRepository]
  private val codeInformationConnector = new RetrieveCnCodeInformationConnector(repository)

  given HeaderCarrier = HeaderCarrier()

  override def beforeEach() = {
    reset(repository)
  }

  "RetrieveCnCodeInformationConnector.retrieveCnCodeInformation" when {
    "Given a List of CnInformation Items, each with a product code and excise code" should {
      "Return a Map of Cncode to CnCodeInformation " in {
        when(repository.fetchCnCodeInformation(any()))
          .thenReturn(
            Future.successful(
              Map(testCnCode1 -> testCnCodeInformation1, testCnCode2 -> testCnCodeInformation2)
            )
          )

        codeInformationConnector
          .retrieveCnCodeInformation(
            testCnCodeInformationRequest
          )
          .map(
            _ shouldBe Right(
              Map(testCnCode1 -> testCnCodeInformation1, testCnCode2 -> testCnCodeInformation2)
            )
          )
      }

    }
    "given an invalid excise code to CnCode combination" should {
      "Return an empty Map" in {
        when(repository.fetchCnCodeInformation(any()))
          .thenReturn(Future.successful(Map.empty))

        codeInformationConnector
          .retrieveCnCodeInformation(
            CnInformationRequest(List(CnInformationItem("M400", "invalid")))
          )
          .map(_ shouldBe Right(Map.empty))

      }
    }
    "return an Error Response" when {
      "there is a error fetching data" in {
        when(repository.fetchCnCodeInformation(any()))
          .thenReturn(Future.failed(new RuntimeException("Simulated failure")))

        codeInformationConnector
          .retrieveCnCodeInformation(testCnCodeInformationRequest)
          .map(_ shouldBe Left(ErrorResponse.UnexpectedDownstreamResponseError))
      }
    }
  }

}
