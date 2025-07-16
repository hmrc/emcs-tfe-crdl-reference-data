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

package uk.gov.hmrc.emcstfereferencedata.connector.retrievePackagingTypes

import org.mockito.ArgumentMatchers.any
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.emcstfereferencedata.config.AppConfig
import uk.gov.hmrc.emcstfereferencedata.models.crdl.{CodeListCode, CrdlCodeListEntry}
import uk.gov.hmrc.emcstfereferencedata.models.response.{ErrorResponse, PackagingType}
import play.api.libs.json.Json
import uk.gov.hmrc.emcstfereferencedata.connector.crdl.CrdlConnector

class RetrievePackagingTypesConnectorCRDLSpec
  extends AnyWordSpec
  with Matchers
  with ScalaFutures
  with MockitoSugar {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val httpClient: HttpClientV2     = mock[HttpClientV2]
  val config: AppConfig            = mock[AppConfig]
  val crdlConnector: CrdlConnector = mock[CrdlConnector]

  val connector = new RetrievePackagingTypesConnectorCRDL(httpClient, config, crdlConnector)

  "RetrievePackagingTypesConnectorCRDL" should {
    "return a Right(Map) when CrdlConnector returns valid entries" in {

      val crdlEntries = List(
        CrdlCodeListEntry(
          key = "1A",
          value = "Drum, steel",
          properties = Json.obj(
            "countableFlag"        -> true,
            "actionIdentification" -> "1236"
          )
        ),
        CrdlCodeListEntry(
          key = "1B",
          value = "Drum, aluminium",
          properties = Json.obj("countableFlag" -> true, "actionIdentification" -> "1237")
        ),
        CrdlCodeListEntry(
          key = "NE",
          value = "Unpacked or unpackaged",
          properties = Json.obj("countableFlag" -> false, "actionIdentification" -> "1421")
        ),
        CrdlCodeListEntry(
          key = "AE",
          value = "Aerosol",
          properties = Json.obj("countableFlag" -> true, "actionIdentification" -> "1268")
        ),
        CrdlCodeListEntry(
          key = "AM",
          value = "Ampoule, non protected",
          properties = Json.obj("countableFlag" -> true, "actionIdentification" -> "1275")
        )
      )

      when(crdlConnector.fetchCodeList(any())(using any()))
        .thenReturn(Future.successful(crdlEntries))

      val result = connector.retrievePackagingTypes().futureValue

      result shouldBe Right(
        Map(
          "1A" -> PackagingType("1A", "Drum, steel", isCountable = true),
          "1B" -> PackagingType("1B", "Drum, aluminium", isCountable = true),
          "NE" -> PackagingType("NE", "Unpacked or unpackaged", isCountable = false),
          "AE" -> PackagingType("AE", "Aerosol", isCountable = true),
          "AM" -> PackagingType("AM", "Ampoule, non protected", isCountable = true)
        )
      )
    }

    "return a Left(ErrorResponse) when property json object empty" in {

      val noCountableFlagEntry = CrdlCodeListEntry(
        key = "1B",
        value = "Drum, aluminium",
        properties = Json.obj()
      )

      when(crdlConnector.fetchCodeList(any())(using any()))
        .thenReturn(Future.successful(List(noCountableFlagEntry)))

      val result = connector.retrievePackagingTypes().futureValue

      result shouldBe Left(ErrorResponse.JsonValidationError)
    }
  }

  "return a Left(ErrorResponse) when receiving invalid property object and parsing fails" in {

    val unexpectedPropertiesEntries = CrdlCodeListEntry(
      key = "1B",
      value = "Drum, aluminium",
      properties = Json.obj("otherFlag" -> false, "actionIdentification" -> "1237")
    )

    when(crdlConnector.fetchCodeList(any())(using any()))
      .thenReturn(Future.successful(List(unexpectedPropertiesEntries)))

    val result = connector.retrievePackagingTypes().futureValue

    result shouldBe Left(ErrorResponse.JsonValidationError)
  }

  "return a Left(ErrorResponse) when unable to receive data from crdl-cache" in {

    when(crdlConnector.fetchCodeList(any())(using any()))
      .thenReturn(Future.failed(UpstreamErrorResponse("Service unavailable", 503)))

    val result = connector.retrievePackagingTypes().futureValue

    result shouldBe Left(ErrorResponse.UnexpectedDownstreamResponseError)
  }
}
