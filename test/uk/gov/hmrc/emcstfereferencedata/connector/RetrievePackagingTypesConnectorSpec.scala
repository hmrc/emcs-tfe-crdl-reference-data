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

import org.mockito.ArgumentMatchers.{any, eq as equalTo}
import org.mockito.Mockito.*
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import uk.gov.hmrc.emcstfereferencedata.connector.crdl.CrdlConnector
import uk.gov.hmrc.emcstfereferencedata.models.crdl.CrdlCodeListEntry
import uk.gov.hmrc.emcstfereferencedata.models.response.ErrorResponse
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ExecutionContext, Future}

class RetrievePackagingTypesConnectorSpec
  extends AnyWordSpec
    with Matchers
    with ScalaFutures
    with MockitoSugar
    with BeforeAndAfterEach {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val crdlConnector: CrdlConnector = mock[CrdlConnector]

  val connector = new RetrievePackagingTypesConnector(crdlConnector)
  val crdlEntries = List(
    CrdlCodeListEntry(
      key = "1A",
      value = "Drum, steel",
      properties = Json.obj(
        "countableFlag" -> true,
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

  override def beforeEach() = {
    reset(crdlConnector)
  }

  "RetrievePackagingTypesConnector" should {
    "return a Right(Map) when CrdlConnector returns valid entries and isCountable parameter is not provided" in {
      when(crdlConnector.fetchCodeList(any(), equalTo(None), equalTo(None))(using any(), any()))
        .thenReturn(Future.successful(crdlEntries))

      val result = connector.retrievePackagingTypes(packagingTypeCodes = None, isCountable = None).futureValue

      result shouldBe Right(
        Map(
          "1A" -> "Drum, steel",
          "1B" -> "Drum, aluminium",
          "NE" -> "Unpacked or unpackaged",
          "AE" -> "Aerosol",
          "AM" -> "Ampoule, non protected"
        )
      )
    }

    "return a Right(Map) when CrdlConnector returns valid entries and isCountable parameter is true" in {
      when(crdlConnector.fetchCodeList(any(), equalTo(None), equalTo(Some(Map("countableFlag"->true))))(using any(), any()))
        .thenReturn(Future.successful(crdlEntries))

      val result = connector.retrievePackagingTypes(packagingTypeCodes = None, isCountable = Some(true)).futureValue

      result shouldBe a[Right[_,_]]
    }

    "return a Right(Map) when CrdlConnector returns valid entries and isCountable parameter is false" in {
      when(crdlConnector.fetchCodeList(any(), equalTo(None), equalTo(Some(Map("countableFlag"->false))))(using any(), any()))
        .thenReturn(Future.successful(crdlEntries))

      val result = connector.retrievePackagingTypes(packagingTypeCodes = None, isCountable = Some(false)).futureValue

      result shouldBe a[Right[_,_]]
    }
  }

  "return a Left(ErrorResponse) when unable to receive data from crdl-cache" in {

    when(crdlConnector.fetchCodeList(any(), equalTo(None), equalTo(None))(using any(), any()))
      .thenReturn(Future.failed(UpstreamErrorResponse("Service unavailable", 503)))

    val result = connector.retrievePackagingTypes(packagingTypeCodes = None, isCountable = None).futureValue

    result shouldBe Left(ErrorResponse.UnexpectedDownstreamResponseError)
  }

}
