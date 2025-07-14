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
import uk.gov.hmrc.emcstfereferencedata.fixtures.PackagingTypeFixtures
import uk.gov.hmrc.emcstfereferencedata.models.response.ErrorResponse.{
  NoDataReturnedFromDatabaseError,
  UnexpectedDownstreamResponseError
}
import uk.gov.hmrc.emcstfereferencedata.services.RetrievePackagingTypesService
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HttpResponse, StringContextOps}

import scala.concurrent.Future

class RetrievePackagingTypesControllerSpec
  extends ControllerIntegrationSpec
  with PackagingTypeFixtures {

  private val authAction            = mock[AuthAction]
  private val packagingTypesService = mock[RetrievePackagingTypesService]

  override def beforeEach(): Unit = {
    reset(authAction)
    reset(packagingTypesService)
  }

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .overrides(
        bind[AuthAction].toInstance(authAction),
        bind[RetrievePackagingTypesService].toInstance(packagingTypesService),
        bind[HttpClientV2].toInstance(httpClientV2)
      )
      .build()

  "RetrievePackagingTypesController.showAllPackagingTypes" should {
    "return 200 OK" when {
      "the service returns packaging types that are countable" in {
        when(authAction(any())).thenReturn(FakeSuccessAuthAction(None))

        when(
          packagingTypesService.retrievePackagingTypes(isCountable = equalTo(Some(true)))(using
            any(),
            any()
          )
        )
          .thenReturn(Future.successful(Right(testPackagingTypesServiceResult)))

        val response =
          httpClientV2
            .get(
              url"http://localhost:$port/emcs-tfe-crdl-reference-data/oracle/packaging-types?isCountable=true"
            )
            .execute[HttpResponse]
            .futureValue

        response.json.as[Map[String, String]] shouldBe testPackagingTypesServiceResult
        response.status shouldBe Status.OK
      }

      "the service returns packaging types that are not countable" in {
        when(authAction(any())).thenReturn(FakeSuccessAuthAction(None))

        when(
          packagingTypesService.retrievePackagingTypes(isCountable = equalTo(Some(false)))(using
            any(),
            any()
          )
        )
          .thenReturn(Future.successful(Right(testPackagingTypesServiceResult)))

        val response =
          httpClientV2
            .get(
              url"http://localhost:$port/emcs-tfe-crdl-reference-data/oracle/packaging-types?isCountable=false"
            )
            .execute[HttpResponse]
            .futureValue

        response.json.as[Map[String, String]] shouldBe testPackagingTypesServiceResult
        response.status shouldBe Status.OK
      }

      "the service returns packaging types that are both countable and not countable" in {
        when(authAction(any())).thenReturn(FakeSuccessAuthAction(None))

        when(
          packagingTypesService.retrievePackagingTypes(isCountable = equalTo(None))(using
            any(),
            any()
          )
        )
          .thenReturn(Future.successful(Right(testPackagingTypesServiceResult)))

        val response =
          httpClientV2
            .get(url"http://localhost:$port/emcs-tfe-crdl-reference-data/oracle/packaging-types")
            .execute[HttpResponse]
            .futureValue

        response.json.as[Map[String, String]] shouldBe testPackagingTypesServiceResult
        response.status shouldBe Status.OK
      }
    }

    "return 403 Forbidden" when {
      "the caller is not authenticated" in {
        when(authAction(any())).thenReturn(FakeFailedAuthAction(None))

        val response =
          httpClientV2
            .get(url"http://localhost:$port/emcs-tfe-crdl-reference-data/oracle/packaging-types")
            .execute[HttpResponse]
            .futureValue

        response.status shouldBe Status.FORBIDDEN
      }
    }

    "return 500 Internal Service Error" when {
      "the connector returns a NoDataReturnedFromDatabaseError" in {
        when(authAction(any())).thenReturn(FakeSuccessAuthAction(None))
        when(packagingTypesService.retrievePackagingTypes(equalTo(None))(using any(), any()))
          .thenReturn(Future.successful(Left(NoDataReturnedFromDatabaseError)))

        val response =
          httpClientV2
            .get(url"http://localhost:$port/emcs-tfe-crdl-reference-data/oracle/packaging-types")
            .execute[HttpResponse]
            .futureValue

        response.status shouldBe Status.INTERNAL_SERVER_ERROR
      }

      "the connector returns an UnexpectedDownstreamResponseError" in {
        when(authAction(any())).thenReturn(FakeSuccessAuthAction(None))
        when(packagingTypesService.retrievePackagingTypes(equalTo(None))(using any(), any()))
          .thenReturn(Future.successful(Left(UnexpectedDownstreamResponseError)))

        val response =
          httpClientV2
            .get(url"http://localhost:$port/emcs-tfe-crdl-reference-data/oracle/packaging-types")
            .execute[HttpResponse]
            .futureValue

        response.status shouldBe Status.INTERNAL_SERVER_ERROR
      }

      "the connector throws an error" in {
        when(authAction(any())).thenReturn(FakeSuccessAuthAction(None))
        when(packagingTypesService.retrievePackagingTypes(equalTo(None))(using any(), any()))
          .thenReturn(Future.failed(new RuntimeException("Boom!")))

        val response =
          httpClientV2
            .get(url"http://localhost:$port/emcs-tfe-crdl-reference-data/oracle/packaging-types")
            .execute[HttpResponse]
            .futureValue

        response.status shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }

  "RetrievePackagingTypesController.show" should {
    "return 200 OK" when {
      "the service returns the requested packaging types" in {
        when(authAction(any())).thenReturn(FakeSuccessAuthAction(None))

        when(
          packagingTypesService.retrievePackagingTypes(equalTo(testPackagingTypes))(using
            any(),
            any()
          )
        )
          .thenReturn(Future.successful(Right(testPackagingTypesServiceResult)))

        val response =
          httpClientV2
            .post(url"http://localhost:$port/emcs-tfe-crdl-reference-data/oracle/packaging-types")
            .withBody(Json.toJson(testPackagingTypes))
            .execute[HttpResponse]
            .futureValue

        response.json.as[Map[String, String]] shouldBe testPackagingTypesServiceResult
        response.status shouldBe Status.OK
      }

    }

    "return 403 Forbidden" when {
      "the caller is not authenticated" in {
        when(authAction(any())).thenReturn(FakeFailedAuthAction(None))

        val response =
          httpClientV2
            .post(url"http://localhost:$port/emcs-tfe-crdl-reference-data/oracle/packaging-types")
            .withBody(Json.toJson(testPackagingTypes))
            .execute[HttpResponse]
            .futureValue

        response.status shouldBe Status.FORBIDDEN
      }
    }

    "return 500 Internal Service Error" when {
      "the connector returns a NoDataReturnedFromDatabaseError" in {
        when(authAction(any())).thenReturn(FakeSuccessAuthAction(None))

        when(
          packagingTypesService.retrievePackagingTypes(equalTo(testPackagingTypes))(using
            any(),
            any()
          )
        )
          .thenReturn(Future.successful(Left(NoDataReturnedFromDatabaseError)))

        val response =
          httpClientV2
            .post(url"http://localhost:$port/emcs-tfe-crdl-reference-data/oracle/packaging-types")
            .withBody(Json.toJson(testPackagingTypes))
            .execute[HttpResponse]
            .futureValue

        response.status shouldBe Status.INTERNAL_SERVER_ERROR
      }

      "the connector returns an UnexpectedDownstreamResponseError" in {
        when(authAction(any())).thenReturn(FakeSuccessAuthAction(None))

        when(
          packagingTypesService.retrievePackagingTypes(equalTo(testPackagingTypes))(using
            any(),
            any()
          )
        )
          .thenReturn(Future.successful(Left(UnexpectedDownstreamResponseError)))

        val response =
          httpClientV2
            .post(url"http://localhost:$port/emcs-tfe-crdl-reference-data/oracle/packaging-types")
            .withBody(Json.toJson(testPackagingTypes))
            .execute[HttpResponse]
            .futureValue

        response.status shouldBe Status.INTERNAL_SERVER_ERROR
      }

      "the connector throws an error" in {
        when(authAction(any())).thenReturn(FakeSuccessAuthAction(None))

        when(
          packagingTypesService.retrievePackagingTypes(equalTo(testPackagingTypes))(using
            any(),
            any()
          )
        )
          .thenReturn(Future.failed(new RuntimeException("Boom!")))

        val response =
          httpClientV2
            .post(url"http://localhost:$port/emcs-tfe-crdl-reference-data/oracle/packaging-types")
            .withBody(Json.toJson(testPackagingTypes))
            .execute[HttpResponse]
            .futureValue

        response.status shouldBe Status.INTERNAL_SERVER_ERROR
      }
    }
  }
}
