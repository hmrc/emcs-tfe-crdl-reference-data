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

import uk.gov.hmrc.emcstfereferencedata.fixtures.BaseFixtures
import uk.gov.hmrc.emcstfereferencedata.mocks.connectors.MockRetrieveOtherReferenceDataConnector
import uk.gov.hmrc.emcstfereferencedata.support.UnitSpec
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.Future

class RetrieveMemberStatesAndCountriesServiceSpec extends UnitSpec with MockRetrieveOtherReferenceDataConnector with BaseFixtures {

  object TestService extends RetrieveMemberStatesAndCountriesService(mockConnector)

  "The RetrieveMemberStatesAndCountriesService" should {
    "return a successful response" when {
      "retrieveMemberStates method returns data and retrieveCountries method returns no data" in {
        MockConnector.retrieveMemberStates()(Future.successful(memberStatesResult))
        MockConnector.retrieveCountries()(Future.successful(Map()))
        await(TestService.get()) shouldBe memberStatesAndCountriesResultNoCountries
      }
      "retrieveMemberStates method returns no data and retrieveCountries method returns data" in {
        MockConnector.retrieveMemberStates()(Future.successful(Map()))
        MockConnector.retrieveCountries()(Future.successful(countriesResult))
        await(TestService.get()) shouldBe memberStatesAndCountriesResultNoMemberStates
      }
      "retrieveMemberStates method returns data and retrieveCountries method returns data" in {
        MockConnector.retrieveMemberStates()(Future.successful(memberStatesResult))
        MockConnector.retrieveCountries()(Future.successful(countriesResult))
        await(TestService.get()) shouldBe memberStatesAndCountriesResult
      }
    }

    "rethrow errors" when {
      "the retrieveMemberStates throws an exception" in {
        MockConnector.retrieveMemberStates()(Future.failed(UpstreamErrorResponse("InternalServerError", 500, 502)))
        MockConnector.retrieveCountries()(Future.successful(Map()))
        assertThrows[UpstreamErrorResponse]{await(TestService.get())}
      }

      "the retrieveCountries throws an exception" in {
        MockConnector.retrieveCountries()(Future.failed(UpstreamErrorResponse("InternalServerError", 500, 502)))
        MockConnector.retrieveMemberStates()(Future.successful(Map()))
        assertThrows[UpstreamErrorResponse]{await(TestService.get())}
      }

    }
  }
}
