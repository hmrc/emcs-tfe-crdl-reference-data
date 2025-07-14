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

import org.mockito.ArgumentMatchers.{any, eq as equalTo}
import org.mockito.Mockito.{reset, when}
import play.api.Application
import play.api.http.Status
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import uk.gov.hmrc.emcstfereferencedata.controllers.predicates.AuthAction
import uk.gov.hmrc.emcstfereferencedata.models.response.CnCodeInformation
import uk.gov.hmrc.emcstfereferencedata.models.response.ErrorResponse.{
  NoDataReturnedFromDatabaseError,
  UnexpectedDownstreamResponseError
}
import uk.gov.hmrc.emcstfereferencedata.services.RetrieveCnCodeInformationService
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HttpResponse, StringContextOps}

import scala.concurrent.Future

class RetrieveCnCodeInformationControllerSpec extends ControllerIntegrationSpec {

  private val authAction     = mock[AuthAction]
  private val cnCodesService = mock[RetrieveCnCodeInformationService]

  override def beforeEach(): Unit = {
    reset(authAction)
    reset(cnCodesService)
  }

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .overrides(
        bind[AuthAction].toInstance(authAction),
        bind[RetrieveCnCodeInformationService].toInstance(cnCodesService),
        bind[HttpClientV2].toInstance(httpClientV2)
      )
      .build()

  private val cnCodeInformationRequest = Json.obj(
    "items" -> Json.arr(
      Json.obj(
        "productCode" -> testProductCode1,
        "cnCode"      -> testCnCode1
      ),
      Json.obj(
        "productCode" -> testProductCode2,
        "cnCode"      -> testCnCode2
      )
    )
  )

  private val cnCodeInformationResponse = Map(
    testCnCode1 -> testCnCodeInformation1,
    testCnCode2 -> testCnCodeInformation2
  )

  "RetrieveCnCodeInformationController" should {
    "return 200 OK" when {
      "the service returns CN code information" in {
        when(authAction(any())).thenReturn(FakeSuccessAuthAction(None))

        when(
          cnCodesService.retrieveCnCodeInformation(equalTo(testCnCodeInformationRequest))(using
            any(),
            any()
          )
        )
          .thenReturn(Future.successful(Right(cnCodeInformationResponse)))

        val response =
          httpClientV2
            .post(
              url"http://localhost:$port/emcs-tfe-crdl-reference-data/oracle/cn-code-information"
            )
            .withBody(cnCodeInformationRequest)
            .execute[HttpResponse]
            .futureValue

        response.json.as[Map[String, CnCodeInformation]] shouldBe cnCodeInformationResponse
        response.status shouldBe Status.OK
      }

    }

    "return 403 Forbidden" when {
      "the caller is not authenticated" in {
        when(authAction(any())).thenReturn(FakeFailedAuthAction(None))

        val response =
          httpClientV2
            .post(
              url"http://localhost:$port/emcs-tfe-crdl-reference-data/oracle/cn-code-information"
            )
            .withBody(cnCodeInformationRequest)
            .execute[HttpResponse]
            .futureValue

        response.status shouldBe Status.FORBIDDEN
      }
    }

    "return 500 Internal Service Error" when {
      "the connector returns a NoDataReturnedFromDatabaseError" in {
        when(authAction(any())).thenReturn(FakeSuccessAuthAction(None))
        when(cnCodesService.retrieveCnCodeInformation(any())(using any(), any()))
          .thenReturn(Future.successful(Left(NoDataReturnedFromDatabaseError)))

        val response =
          httpClientV2
            .post(
              url"http://localhost:$port/emcs-tfe-crdl-reference-data/oracle/cn-code-information"
            )
            .withBody(cnCodeInformationRequest)
            .execute[HttpResponse]
            .futureValue

        response.status shouldBe Status.INTERNAL_SERVER_ERROR
      }

      "the connector returns an UnexpectedDownstreamResponseError" in {
        when(authAction(any())).thenReturn(FakeSuccessAuthAction(None))
        when(cnCodesService.retrieveCnCodeInformation(any())(using any(), any()))
          .thenReturn(Future.successful(Left(UnexpectedDownstreamResponseError)))

        val response =
          httpClientV2
            .post(
              url"http://localhost:$port/emcs-tfe-crdl-reference-data/oracle/cn-code-information"
            )
            .withBody(cnCodeInformationRequest)
            .execute[HttpResponse]
            .futureValue

        response.status shouldBe Status.INTERNAL_SERVER_ERROR
      }

      "the connector throws an error" in {
        when(authAction(any())).thenReturn(FakeSuccessAuthAction(None))
        when(cnCodesService.retrieveCnCodeInformation(any())(using any(), any()))
          .thenReturn(Future.failed(new RuntimeException("Boom!")))

        val response =
          httpClientV2
            .post(
              url"http://localhost:$port/emcs-tfe-crdl-reference-data/oracle/cn-code-information"
            )
            .withBody(cnCodeInformationRequest)
            .execute[HttpResponse]
            .futureValue

        response.status shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }
}
