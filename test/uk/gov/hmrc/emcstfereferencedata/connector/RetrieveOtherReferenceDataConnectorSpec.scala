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
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import uk.gov.hmrc.emcstfereferencedata.connector.crdl.CrdlConnector
import uk.gov.hmrc.emcstfereferencedata.fixtures.BaseFixtures
import uk.gov.hmrc.emcstfereferencedata.models.crdl.CodeListCode.BC35
import uk.gov.hmrc.emcstfereferencedata.models.crdl.{CodeListCode, CrdlCodeListEntry}
import uk.gov.hmrc.emcstfereferencedata.models.response.ErrorResponse
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

class RetrieveOtherReferenceDataConnectorSpec
  extends AsyncWordSpec
  with Matchers
  with MockitoSugar
  with BaseFixtures
  with BeforeAndAfterEach {
  private val crdlConnector = mock[CrdlConnector]
  private val connector     = new RetrieveOtherReferenceDataConnector(crdlConnector)
  private val codeListCode  = BC35

  given HeaderCarrier = HeaderCarrier()

  def convertToCrdlCodeListEntrySeq(
    resultMap: Map[String, String]
  ): Seq[CrdlCodeListEntry] = {
    resultMap.map { case (k, v) =>
      CrdlCodeListEntry(k, v, Json.obj())
    }.toSeq
  }

  override def beforeEach() = {
    reset(crdlConnector)
  }

  "RetrieveOtherReferenceDataConnector.retrieveOtherReferenceData " should {
    "given a typeName return a map of transportUnits in key and value pairs" in {
      val codeListEntrySeq = convertToCrdlCodeListEntrySeq(transportUnitsResult)

      when(crdlConnector.fetchCodeList(any(), equalTo(None), equalTo(None))(using any(), any()))
        .thenReturn(Future.successful(codeListEntrySeq))

      connector
        .retrieveOtherReferenceData(codeListCode, filterKeys = None)
        .map(_ shouldBe Right(transportUnitsResult))

    }

    "given an invalid codelist code return a empty list" in {
      when(crdlConnector.fetchCodeList(any(), equalTo(None), equalTo(None))(using any(), any()))
        .thenReturn(Future.successful(Seq.empty))

      connector
        .retrieveOtherReferenceData(codeListCode, filterKeys = None)
        .map(_ shouldBe Right(Map.empty))

    }

    "when there is an error fetching data return an error response" in {
      when(crdlConnector.fetchCodeList(any(), equalTo(None), equalTo(None))(using any(), any()))
        .thenReturn(Future.failed(new RuntimeException("Simulated failure")))

      connector
        .retrieveOtherReferenceData(codeListCode, filterKeys = None)
        .map(_ shouldBe Left(ErrorResponse.UnexpectedDownstreamResponseError))
    }
  }
  "RetrieveOtherReferenceDataConnector.retrieveWineOperations" should {
    "given a codelist code BC41 (WineOperations) return a map of wineOperations in key and value pairs" in {
      val codeListEntrySeq = convertToCrdlCodeListEntrySeq(testWineOperationsResult)

      when(
        crdlConnector.fetchCodeList(CodeListCode(equalTo("BC41")), equalTo(None), equalTo(None))(
          using
          any(),
          any()
        )
      )
        .thenReturn(Future.successful(codeListEntrySeq))

      connector
        .retrieveWineOperations(filterKeys = None)
        .map(_ shouldBe Right(testWineOperationsResult))
    }
  }
  "RetrieveOtherReferenceDataConnector.retrieveMemberStates" should {
    "given a codelist code BC11 (MemberStates) return a map of memberStates in key and value pairs" in {
      val codeListEntrySeq = convertToCrdlCodeListEntrySeq(memberStatesResult)

      when(
        crdlConnector.fetchCodeList(CodeListCode(equalTo("BC11")), equalTo(None), equalTo(None))(
          using
          any(),
          any()
        )
      )
        .thenReturn(Future.successful(codeListEntrySeq))

      connector.retrieveMemberStates().map(_ shouldBe Right(memberStatesResult))
    }
  }

  "RetrieveOtherReferenceDataConnector.retrieveCountries" should {
    "given a codelist code BC08 (Countries) return a map of countries in key and value pairs" in {
      val codeListEntrySeq = convertToCrdlCodeListEntrySeq(countriesResult)

      when(
        crdlConnector.fetchCodeList(CodeListCode(equalTo("BC08")), equalTo(None), equalTo(None))(
          using
          any(),
          any()
        )
      )
        .thenReturn(Future.successful(codeListEntrySeq))

      connector.retrieveCountries().map(_ shouldBe Right(countriesResult))
    }
  }

  "RetrieveOtherReferenceDataConnector.retrieveTransportUnits" should {
    "given a codelist code BC35 (TransportUnits) return a map of transportUnits in key and value pairs" in {
      val codeListEntrySeq = convertToCrdlCodeListEntrySeq(transportUnitsResult)

      when(
        crdlConnector.fetchCodeList(CodeListCode(equalTo("BC35")), equalTo(None), equalTo(None))(using any(), any())
      )
        .thenReturn(Future.successful(codeListEntrySeq))

      connector.retrieveTransportUnits().map(_ shouldBe Right(transportUnitsResult))
    }
  }

  "RetrieveOtherReferenceDataConnector.retrieveTypesOfDocument" should {
    "given a codelist code BC106 (TypeOfDocument) return a map of typeOfDocument in key and value pairs" in {
      val codeListEntrySeq = convertToCrdlCodeListEntrySeq(typesOfDocumentResult)

      when(
        crdlConnector.fetchCodeList(CodeListCode(equalTo("BC106")), equalTo(None), equalTo(None))(using
          any(),
          any()
        )
      )
        .thenReturn(Future.successful(codeListEntrySeq))

      connector.retrieveTypesOfDocument().map(_ shouldBe Right(typesOfDocumentResult))
    }
  }

}
