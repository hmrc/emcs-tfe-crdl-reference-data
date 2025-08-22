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

package uk.gov.hmrc.emcstfereferencedata.services

import org.mockito.ArgumentMatchers.{any, eq as equalTo}
import org.mockito.Mockito.{reset, when}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json
import uk.gov.hmrc.emcstfereferencedata.connector.CrdlConnector
import uk.gov.hmrc.emcstfereferencedata.fixtures.BaseFixtures
import uk.gov.hmrc.emcstfereferencedata.models.crdl.CodeListCode.BC35
import uk.gov.hmrc.emcstfereferencedata.models.crdl.{CodeListCode, CrdlCodeListEntry}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import scala.concurrent.Future

class RetrieveOtherReferenceDataServiceSpec
  extends AsyncWordSpec
  with Matchers
  with MockitoSugar
  with BaseFixtures
  with BeforeAndAfterEach {
  private val crdlConnector = mock[CrdlConnector]
  private val service       = new RetrieveOtherReferenceDataService(crdlConnector)
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

      service
        .retrieveOtherReferenceData(codeListCode, filterKeys = None)
        .map(_ shouldBe transportUnitsResult)

    }

    "given an invalid codelist code return a empty list" in {
      when(crdlConnector.fetchCodeList(any(), equalTo(None), equalTo(None))(using any(), any()))
        .thenReturn(Future.successful(Seq.empty))

      service
        .retrieveOtherReferenceData(codeListCode, filterKeys = None)
        .map(_ shouldBe Map.empty)

    }

    "when there is an error fetching data return an UpstreamErrorResponse" in {
      when(crdlConnector.fetchCodeList(any(), equalTo(None), equalTo(None))(using any(), any()))
        .thenReturn(Future.failed(UpstreamErrorResponse("Service unavailable", 503)))

      recoverToSucceededIf[UpstreamErrorResponse] {
        service.retrieveOtherReferenceData(codeListCode, filterKeys = None)
      }
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

      service
        .retrieveWineOperations(filterKeys = None)
        .map(_ shouldBe testWineOperationsResult)
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

      service.retrieveMemberStates().map(_ shouldBe memberStatesResult)
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

      service.retrieveCountries().map(_ shouldBe countriesResult)
    }
  }

  "RetrieveOtherReferenceDataConnector.retrieveTransportUnits" should {
    "given a codelist code BC35 (TransportUnits) return a map of transportUnits in key and value pairs" in {
      val codeListEntrySeq = convertToCrdlCodeListEntrySeq(transportUnitsResult)

      when(
        crdlConnector.fetchCodeList(CodeListCode(equalTo("BC35")), equalTo(None), equalTo(None))(
          using
          any(),
          any()
        )
      )
        .thenReturn(Future.successful(codeListEntrySeq))

      service.retrieveTransportUnits().map(_ shouldBe transportUnitsResult)
    }
  }

  "RetrieveOtherReferenceDataConnector.retrieveTypesOfDocument" should {
    "given a codelist code BC106 (TypeOfDocument) return a map of typeOfDocument in key and value pairs" in {
      val codeListEntrySeq = convertToCrdlCodeListEntrySeq(typesOfDocumentResult)

      when(
        crdlConnector.fetchCodeList(CodeListCode(equalTo("BC106")), equalTo(None), equalTo(None))(
          using
          any(),
          any()
        )
      )
        .thenReturn(Future.successful(codeListEntrySeq))

      service.retrieveTypesOfDocument().map(_ shouldBe typesOfDocumentResult)
    }
  }

  "RetrieveOtherReferenceDataConnector.retrieveMemberStatesAndCountries" should {
    "return a successful response" when {
      "retrieveMemberStates method returns data and retrieveCountries method returns no data" in {
        val codeListEntrySeq = convertToCrdlCodeListEntrySeq(memberStatesResult)

        when(
          crdlConnector.fetchCodeList(CodeListCode(equalTo("BC11")), equalTo(None), equalTo(None))(
            using
            any(),
            any()
          )
        )
          .thenReturn(Future.successful(codeListEntrySeq))

        when(
          crdlConnector.fetchCodeList(CodeListCode(equalTo("BC08")), equalTo(None), equalTo(None))(
            using
            any(),
            any()
          )
        )
          .thenReturn(Future.successful(List.empty))
        service
          .retrieveMemberStatesAndCountries()
          .map(_ shouldBe memberStatesAndCountriesResultNoCountries)
      }

      "retrieveMemberStates method returns no data and retrieveCountries method returns data" in {
        when(
          crdlConnector.fetchCodeList(CodeListCode(equalTo("BC11")), equalTo(None), equalTo(None))(
            using
            any(),
            any()
          )
        )
          .thenReturn(Future.successful(List.empty))
        val codeListEntrySeq = convertToCrdlCodeListEntrySeq(countriesResult)

        when(
          crdlConnector.fetchCodeList(CodeListCode(equalTo("BC08")), equalTo(None), equalTo(None))(
            using
            any(),
            any()
          )
        )
          .thenReturn(Future.successful(codeListEntrySeq))
        service
          .retrieveMemberStatesAndCountries()
          .map(_ shouldBe memberStatesAndCountriesResultNoMemberStates)
      }
      "retrieveMemberStates method returns data and retrieveCountries method returns data" in {
        val memberStatesSeq = convertToCrdlCodeListEntrySeq(memberStatesResult)

        when(
          crdlConnector.fetchCodeList(CodeListCode(equalTo("BC11")), equalTo(None), equalTo(None))(
            using
            any(),
            any()
          )
        )
          .thenReturn(Future.successful(memberStatesSeq))
        val countriesSeq = convertToCrdlCodeListEntrySeq(countriesResult)

        when(
          crdlConnector.fetchCodeList(CodeListCode(equalTo("BC08")), equalTo(None), equalTo(None))(
            using
            any(),
            any()
          )
        )
          .thenReturn(Future.successful(countriesSeq))
        service.retrieveMemberStatesAndCountries().map(_ shouldBe memberStatesAndCountriesResult)
      }
    }
    "rethrow errors" when {
      "the retrieveMemberStates throws an exception" in {
        when(
          crdlConnector.fetchCodeList(CodeListCode(equalTo("BC11")), equalTo(None), equalTo(None))(
            using
            any(),
            any()
          )
        )
          .thenReturn(Future.failed(UpstreamErrorResponse("InternalServerError", 500, 502)))
        when(
          crdlConnector.fetchCodeList(CodeListCode(equalTo("BC08")), equalTo(None), equalTo(None))(
            using
            any(),
            any()
          )
        )
          .thenReturn(Future.successful(List.empty))
        recoverToSucceededIf[UpstreamErrorResponse] {
          service.retrieveMemberStatesAndCountries()
        }
      }

      "the retrieveCountries throws an exception" in {
        when(
          crdlConnector.fetchCodeList(CodeListCode(equalTo("BC08")), equalTo(None), equalTo(None))(
            using
            any(),
            any()
          )
        ).thenReturn(Future.failed(UpstreamErrorResponse("InternalServerError", 500, 502)))
        when(
          crdlConnector.fetchCodeList(CodeListCode(equalTo("BC11")), equalTo(None), equalTo(None))(
            using
            any(),
            any()
          )
        ).thenReturn(Future.successful(Map()))
        recoverToSucceededIf[UpstreamErrorResponse] {
          service.retrieveMemberStatesAndCountries()
        }
      }

    }
  }

}
