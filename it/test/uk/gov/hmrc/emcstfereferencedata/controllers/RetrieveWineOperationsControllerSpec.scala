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
import uk.gov.hmrc.emcstfereferencedata.models.response.ErrorResponse.{
  NoDataReturnedFromDatabaseError,
  UnexpectedDownstreamResponseError
}
import uk.gov.hmrc.emcstfereferencedata.services.RetrieveWineOperationsService
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HttpResponse, StringContextOps}

import scala.concurrent.Future

class RetrieveWineOperationsControllerSpec extends ControllerIntegrationSpec {

  private val authAction     = mock[AuthAction]
  private val wineOpsService = mock[RetrieveWineOperationsService]

  override def beforeEach(): Unit = {
    reset(authAction)
    reset(wineOpsService)
  }

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .overrides(
        bind[AuthAction].toInstance(authAction),
        bind[RetrieveWineOperationsService].toInstance(wineOpsService),
        bind[HttpClientV2].toInstance(httpClientV2)
      )
      .build()

  "RetrieveWineOperationsController.showAllWineOperations" should {
    "return 200 OK" when {
      "the service returns wine operations" in {
        when(authAction(any())).thenReturn(FakeSuccessAuthAction(None))

        when(wineOpsService.retrieveWineOperations()(using any(), any()))
          .thenReturn(Future.successful(Right(testWineOperationsResult)))

        val response =
          httpClientV2
            .get(url"$baseUrl/oracle/wine-operations")
            .execute[HttpResponse]
            .futureValue

        response.json.as[Map[String, String]] shouldBe testWineOperationsResult
        response.status shouldBe Status.OK
      }
    }

    "return 403 Forbidden" when {
      "the caller is not authenticated" in {
        when(authAction(any())).thenReturn(FakeFailedAuthAction(None))

        val response =
          httpClientV2
            .get(url"$baseUrl/oracle/wine-operations")
            .execute[HttpResponse]
            .futureValue

        response.status shouldBe Status.FORBIDDEN
      }
    }

    "return 500 Internal Service Error" when {
      "the connector returns a NoDataReturnedFromDatabaseError" in {
        when(authAction(any())).thenReturn(FakeSuccessAuthAction(None))
        when(wineOpsService.retrieveWineOperations()(using any(), any()))
          .thenReturn(Future.successful(Left(NoDataReturnedFromDatabaseError)))

        val response =
          httpClientV2
            .get(url"$baseUrl/oracle/wine-operations")
            .execute[HttpResponse]
            .futureValue

        response.status shouldBe Status.INTERNAL_SERVER_ERROR
      }

      "the connector returns an UnexpectedDownstreamResponseError" in {
        when(authAction(any())).thenReturn(FakeSuccessAuthAction(None))
        when(wineOpsService.retrieveWineOperations()(using any(), any()))
          .thenReturn(Future.successful(Left(UnexpectedDownstreamResponseError)))

        val response =
          httpClientV2
            .get(url"$baseUrl/oracle/wine-operations")
            .execute[HttpResponse]
            .futureValue

        response.status shouldBe Status.INTERNAL_SERVER_ERROR
      }

      "the connector throws an error" in {
        when(authAction(any())).thenReturn(FakeSuccessAuthAction(None))
        when(wineOpsService.retrieveWineOperations()(using any(), any()))
          .thenReturn(Future.failed(new RuntimeException("Boom!")))

        val response =
          httpClientV2
            .get(url"$baseUrl/oracle/wine-operations")
            .execute[HttpResponse]
            .futureValue

        response.status shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }

  "RetrieveWineOperationsController.show" should {
    "return 200 OK" when {
      "the service returns the requested wine operations" in {
        when(authAction(any())).thenReturn(FakeSuccessAuthAction(None))

        when(wineOpsService.retrieveWineOperations(equalTo(testWineOperations))(using any(), any()))
          .thenReturn(Future.successful(Right(testWineOperationsResult)))

        val response =
          httpClientV2
            .post(url"$baseUrl/oracle/wine-operations")
            .withBody(Json.toJson(testWineOperations))
            .execute[HttpResponse]
            .futureValue

        response.json.as[Map[String, String]] shouldBe testWineOperationsResult
        response.status shouldBe Status.OK
      }
    }

    "return 403 Forbidden" when {
      "the caller is not authenticated" in {
        when(authAction(any())).thenReturn(FakeFailedAuthAction(None))

        val response =
          httpClientV2
            .post(url"$baseUrl/oracle/wine-operations")
            .withBody(Json.toJson(testWineOperations))
            .execute[HttpResponse]
            .futureValue

        response.status shouldBe Status.FORBIDDEN
      }
    }

    "return 500 Internal Service Error" when {
      "the connector returns a NoDataReturnedFromDatabaseError" in {
        when(authAction(any())).thenReturn(FakeSuccessAuthAction(None))

        when(wineOpsService.retrieveWineOperations(equalTo(testWineOperations))(using any(), any()))
          .thenReturn(Future.successful(Left(NoDataReturnedFromDatabaseError)))

        val response =
          httpClientV2
            .post(url"$baseUrl/oracle/wine-operations")
            .withBody(Json.toJson(testWineOperations))
            .execute[HttpResponse]
            .futureValue

        response.status shouldBe Status.INTERNAL_SERVER_ERROR
      }

      "the connector returns an UnexpectedDownstreamResponseError" in {
        when(authAction(any())).thenReturn(FakeSuccessAuthAction(None))

        when(wineOpsService.retrieveWineOperations(equalTo(testWineOperations))(using any(), any()))
          .thenReturn(Future.successful(Left(UnexpectedDownstreamResponseError)))

        val response =
          httpClientV2
            .post(url"$baseUrl/oracle/wine-operations")
            .withBody(Json.toJson(testWineOperations))
            .execute[HttpResponse]
            .futureValue

        response.status shouldBe Status.INTERNAL_SERVER_ERROR
      }

      "the connector throws an error" in {
        when(authAction(any())).thenReturn(FakeSuccessAuthAction(None))

        when(wineOpsService.retrieveWineOperations(equalTo(testWineOperations))(using any(), any()))
          .thenReturn(Future.failed(new RuntimeException("Boom!")))

        val response =
          httpClientV2
            .post(url"$baseUrl/oracle/wine-operations")
            .withBody(Json.toJson(testWineOperations))
            .execute[HttpResponse]
            .futureValue

        response.status shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }
}
