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

package uk.gov.hmrc.emcstfereferencedata.controllers

import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{reset, when}
import play.api.Application
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import uk.gov.hmrc.emcstfereferencedata.controllers.predicates.AuthAction
import uk.gov.hmrc.emcstfereferencedata.models.response.Country
import uk.gov.hmrc.emcstfereferencedata.services.RetrieveOtherReferenceDataService
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HttpResponse, StringContextOps, UpstreamErrorResponse}

import scala.concurrent.Future

class RetrieveMemberStatesAndCountriesControllerSpec extends ControllerIntegrationSpec {

  private val authAction       = mock[AuthAction]
  private val countriesService = mock[RetrieveOtherReferenceDataService]

  override def beforeEach(): Unit = {
    reset(authAction)
    reset(countriesService)
  }

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .overrides(
        bind[AuthAction].toInstance(authAction),
        bind[RetrieveOtherReferenceDataService].toInstance(countriesService),
        bind[HttpClientV2].toInstance(httpClientV2)
      )
      .build()

  "RetrieveMemberStatesAndCountriesController" should {
    "return 200 OK" when {
      "the service returns countries and member states" in {
        when(authAction(any())).thenReturn(FakeSuccessAuthAction(None))

        when(countriesService.retrieveMemberStatesAndCountries()(using any(), any()))
          .thenReturn(Future.successful(memberStatesAndCountriesResult))

        val response =
          httpClientV2
            .get(url"$baseUrl/oracle/member-states-and-countries")
            .execute[HttpResponse]
            .futureValue

        response.json.as[Seq[Country]] shouldBe memberStatesAndCountriesResult
        response.status shouldBe Status.OK
      }

    }

    "return 403 Forbidden" when {
      "the caller is not authenticated" in {
        when(authAction(any())).thenReturn(FakeFailedAuthAction(None))

        val response =
          httpClientV2
            .get(url"$baseUrl/oracle/member-states-and-countries")
            .execute[HttpResponse]
            .futureValue

        response.status shouldBe Status.FORBIDDEN
      }
    }

    "return 500 Internal Service Error" when {
      "the connector returns no data" in {
        when(authAction(any())).thenReturn(FakeSuccessAuthAction(None))
        when(countriesService.retrieveMemberStatesAndCountries()(using any(), any()))
          .thenReturn(Future.successful(Seq.empty))

        val response =
          httpClientV2
            .get(url"$baseUrl/oracle/member-states-and-countries")
            .execute[HttpResponse]
            .futureValue

        response.status shouldBe Status.INTERNAL_SERVER_ERROR
      }

      "the connector throws an error" in {
        when(authAction(any())).thenReturn(FakeSuccessAuthAction(None))
        when(countriesService.retrieveMemberStatesAndCountries()(using any(), any()))
          .thenReturn(Future.failed(new RuntimeException("Boom!")))

        val response =
          httpClientV2
            .get(url"$baseUrl/oracle/member-states-and-countries")
            .execute[HttpResponse]
            .futureValue

        response.status shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }

    "return the `reportAs` status code" when {
      "the service throws an UpstreamErrorResponse" in {
        when(authAction(any())).thenReturn(FakeSuccessAuthAction(None))

        when(
          countriesService.retrieveMemberStatesAndCountries()(using any(), any())
        ).thenReturn(Future.failed(UpstreamErrorResponse("Internal Server Error", 500, 502)))

        val response =
          httpClientV2
            .get(url"$baseUrl/oracle/member-states-and-countries")
            .execute[HttpResponse]
            .futureValue

        response.status shouldBe Status.BAD_GATEWAY
      }
    }
  }
}
