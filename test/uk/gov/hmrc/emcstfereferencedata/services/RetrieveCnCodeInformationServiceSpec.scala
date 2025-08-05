/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.emcstfereferencedata.services

import org.mockito.Mockito
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.emcstfereferencedata.fixtures.BaseFixtures
import uk.gov.hmrc.emcstfereferencedata.repositories.{CnCodesRepository, ExciseProductsRepository}
import uk.gov.hmrc.emcstfereferencedata.support.UnitSpec

import scala.concurrent.Future

class RetrieveCnCodeInformationServiceSpec
  extends UnitSpec
    with BaseFixtures
    with BeforeAndAfterEach {

  private val cnCodesRepository = Mockito.mock(classOf[CnCodesRepository])
  private val exciseProductsRepository = Mockito.mock(classOf[ExciseProductsRepository])
  private val service = new RetrieveCnCodeInformationService(cnCodesRepository, exciseProductsRepository)

  override def beforeEach(): Unit = {
    reset(cnCodesRepository)
    reset(exciseProductsRepository)
  }

  "The RetrieveCnCodeInformationService" should {
    "return a successful response containing the CnCodeInformation" when {
      "retrieveCnCodeInformation method is called" in {
        val testResponse1 = Map(testCnCode1 -> testCnCodeInformation1)
        val testResponse2 = Map(testCnCode2 -> testCnCodeInformation2)
        when(cnCodesRepository.fetchCnCodeInformation(testCnCodeInformationRequest)).thenReturn(Future.successful(testResponse1))
        when(exciseProductsRepository.fetchProductCodesInformation(testCnCodeInformationRequest.copy(items = Seq(testCnCodeInformationItem2)))).thenReturn(Future.successful(testResponse2))

        await(service.retrieveCnCodeInformation(testCnCodeInformationRequest)) shouldBe Map(testCnCode1 -> testCnCodeInformation1, testCnCode2 -> testCnCodeInformation2)
      }
    }

    "rethrow errors" when {
      "when the cnCodesRepository returns an error" in {
        when(cnCodesRepository.fetchCnCodeInformation(testCnCodeInformationRequest)).thenReturn(Future.failed(RuntimeException("Boom!!")))
        when(exciseProductsRepository.fetchProductCodesInformation(testCnCodeInformationRequest.copy(items = Seq(testCnCodeInformationItem2)))).thenReturn(Future.successful(Map.empty))
        assertThrows[RuntimeException] {
          await(service.retrieveCnCodeInformation(testCnCodeInformationRequest))
        }
      }

      "when the exciseProductsRepository returns an error" in {
        when(cnCodesRepository.fetchCnCodeInformation(testCnCodeInformationRequest)).thenReturn(Future.successful(Map.empty))
        when(exciseProductsRepository.fetchProductCodesInformation(testCnCodeInformationRequest.copy(items = Seq(testCnCodeInformationItem2)))).thenReturn(Future.failed(RuntimeException("Boom!!")))
        assertThrows[RuntimeException] {
          await(service.retrieveCnCodeInformation(testCnCodeInformationRequest))
        }
      }
    }
  }

}
