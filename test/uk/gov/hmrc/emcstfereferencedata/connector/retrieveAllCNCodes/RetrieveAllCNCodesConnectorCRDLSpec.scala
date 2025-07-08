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

package uk.gov.hmrc.emcstfereferencedata.connector.retrieveAllCNCodes

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatestplus.mockito.MockitoSugar
import uk.gov.hmrc.emcstfereferencedata.fixtures.BaseFixtures
import uk.gov.hmrc.emcstfereferencedata.models.response.{CnCodeInformation, ErrorResponse}
import uk.gov.hmrc.emcstfereferencedata.repositories.CnCodesRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class RetrieveAllCNCodesConnectorCRDLSpec
  extends AsyncWordSpec
  with Matchers
  with MockitoSugar
  with BaseFixtures {

  private val repository = mock[CnCodesRepository]
  private val connector  = new RetrieveAllCNCodesConnectorCRDL(repository)

  given HeaderCarrier = HeaderCarrier()

  "RetrieveAllCNCodesConnectorCRDL.retrieveAllCnCodes" when {
    "given an excise product code" should {
      "return a list of CN Code information" in {
        when(repository.fetchCnCodesForProduct(any()))
          .thenReturn(Future.successful(Seq(testCnCodeInformation1)))

        connector.retrieveAllCnCodes("T400").map(_ shouldBe Right(Seq(testCnCodeInformation1)))

      }
    }

    "given an invalid excise product code " should {
      "return a empty list" in {
        when(repository.fetchCnCodesForProduct(any())).thenReturn(Future.successful(Seq.empty))

        connector.retrieveAllCnCodes("doesn't exist").map(_ shouldBe Right(Seq.empty))

      }
    }
    "return an Error Response" when {
      "there is a error fetching data" in {
        when(repository.fetchCnCodesForProduct(any()))
          .thenReturn(Future.failed(new RuntimeException("Simulated failure")))

        connector
          .retrieveAllCnCodes(testCnCode1)
          .map(_ shouldBe Left(ErrorResponse.UnexpectedDownstreamResponseError))
      }
    }
  }

}
