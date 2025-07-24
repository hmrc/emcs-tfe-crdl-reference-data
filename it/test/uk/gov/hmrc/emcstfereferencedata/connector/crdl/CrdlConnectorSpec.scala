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

package uk.gov.hmrc.emcstfereferencedata.connector.crdl

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.stubbing.Scenario
import org.apache.pekko.actor.ActorSystem
import org.scalatest.EitherValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import play.api.Configuration
import play.api.http.{ContentTypes, HeaderNames}
import play.api.libs.json.{Json, Writes}
import uk.gov.hmrc.emcstfereferencedata.config.AppConfig
import uk.gov.hmrc.emcstfereferencedata.models.crdl.{CodeListCode, CrdlCodeListEntry}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.http.test.{HttpClientV2Support, WireMockSupport}
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.ExecutionContext
import uk.gov.hmrc.http.HeaderCarrier

class CrdlConnectorSpec
  extends AsyncWordSpec
  with Matchers
  with WireMockSupport
  with HttpClientV2Support
  with EitherValues {

  given actorSystem: ActorSystem  = ActorSystem("test")
  given ExecutionContext          = actorSystem.dispatcher
  given HeaderCarrier             = HeaderCarrier()
  given Writes[CrdlCodeListEntry] = Json.writes[CrdlCodeListEntry]

  private val config = Configuration(
    "microservice.services.crdl-cache.host" -> "localhost",
    "microservice.services.crdl-cache.path" -> "crdl-cache/lists",
    "microservice.services.crdl-cache.port" -> wireMockPort,
    "http-verbs.retries.intervals"          -> List("1.millis")
  )

  private val appConfig = AppConfig(config, ServicesConfig(config))

  private val connector = new CrdlConnector(appConfig, httpClientV2)

  private val exciseProductCategories = List(
    CrdlCodeListEntry(
      key = "B",
      value = "Beer",
      properties = Json.obj("actionIdentification" -> "1084")
    ),
    CrdlCodeListEntry(
      key = "E",
      value = "Energy Products",
      properties = Json.obj("actionIdentification" -> "1085")
    ),
    CrdlCodeListEntry(
      key = "I",
      value = "Intermediate products",
      properties = Json.obj("actionIdentification" -> "1086")
    ),
    CrdlCodeListEntry(
      key = "S",
      value = "Ethyl alcohol and spirits",
      properties = Json.obj("actionIdentification" -> "1087")
    ),
    CrdlCodeListEntry(
      key = "T",
      value = "Manufactured tobacco products",
      properties = Json.obj("actionIdentification" -> "1088")
    ),
    CrdlCodeListEntry(
      key = "W",
      value = "Wine and fermented beverages other than wine and beer",
      properties = Json.obj("actionIdentification" -> "1089")
    )
  )

  "CrdlConnectorSpec.fetchCodeList" should {
    "return codelist entries when given the code for a codelist" in {
      stubFor(
        get(urlPathEqualTo("/crdl-cache/lists/BC66"))
          .willReturn(
            ok()
              .withHeader(HeaderNames.CONTENT_TYPE, ContentTypes.JSON)
              .withBody(Json.stringify(Json.toJson(exciseProductCategories)))
          )
      )

      connector
        .fetchCodeList(CodeListCode.BC66, filterKeys = None, filterProperties = None)
        .map(_ shouldBe exciseProductCategories)
    }

    "supply a query parameter to filter the keys of entries when keys are provided for filtering" in {
      stubFor(
        get(urlPathEqualTo("/crdl-cache/lists/BC66"))
          .withQueryParam("keys", equalTo("E,I,S"))
          .willReturn(
            ok()
              .withHeader(HeaderNames.CONTENT_TYPE, ContentTypes.JSON)
              .withBody(Json.stringify(Json.toJson(exciseProductCategories)))
          )
      )

      connector
        .fetchCodeList(CodeListCode.BC66, filterKeys = Some(Set("E", "I", "S")), filterProperties = None)
        .map(_ shouldBe exciseProductCategories)
    }

    "supply a query parameter to filter the properties of entries when properties are provided for filtering" in {
      stubFor(
        get(urlPathEqualTo("/crdl-cache/lists/BC66"))
          .withQueryParam("countableFlag", equalTo("true"))
          .willReturn(
            ok()
              .withHeader(HeaderNames.CONTENT_TYPE, ContentTypes.JSON)
              .withBody(Json.stringify(Json.toJson(exciseProductCategories)))
          )
      )

      connector
        .fetchCodeList(CodeListCode.BC66, filterKeys = None, filterProperties = Some(Map("countableFlag"->true)))
        .map(_ shouldBe exciseProductCategories)
    }

    "supply a query parameter to filter the keys and properties of entries when both keys and properties are provided for filtering" in {
      stubFor(
        get(urlPathEqualTo("/crdl-cache/lists/BC66"))
          .withQueryParam("keys", equalTo("E,I,S"))
          .withQueryParam("countableFlag", equalTo("true"))
          .willReturn(
            ok()
              .withHeader(HeaderNames.CONTENT_TYPE, ContentTypes.JSON)
              .withBody(Json.stringify(Json.toJson(exciseProductCategories)))
          )
      )

      connector
        .fetchCodeList(CodeListCode.BC66, filterKeys = Some(Set("E", "I", "S")), filterProperties = Some(Map("countableFlag" -> true)))
        .map(_ shouldBe exciseProductCategories)
    }

    "retry issuing the request when the upstream service returns a server error" in {
      val retrySuccess = "RetrySuccess"
      val failedState  = "Failed"

      stubFor(
        get(urlPathEqualTo("/crdl-cache/lists/BC66"))
          .inScenario(retrySuccess)
          .whenScenarioStateIs(Scenario.STARTED)
          .willReturn(serverError())
          .willSetStateTo(failedState)
      )

      stubFor(
        get(urlPathEqualTo("/crdl-cache/lists/BC66"))
          .inScenario(retrySuccess)
          .whenScenarioStateIs(failedState)
          .willReturn(
            ok()
              .withHeader(HeaderNames.CONTENT_TYPE, ContentTypes.JSON)
              .withBody(Json.stringify(Json.toJson(exciseProductCategories)))
          )
      )

      connector
        .fetchCodeList(CodeListCode.BC66, filterKeys = None, filterProperties = None)
        .map(_ shouldBe exciseProductCategories)
    }

    "rethrow the error when the maximum number of retries has been exceeded" in {
      stubFor(
        get(urlPathEqualTo("/crdl-cache/lists/BC66"))
          .willReturn(serverError())
      )

      recoverToSucceededIf[UpstreamErrorResponse] {
        connector.fetchCodeList(CodeListCode.BC66, filterKeys = None, filterProperties = None)
      }
    }

    "rethrow the error without retries when the upstream service returns a client error" in {
      val shouldNotRetry = "ShouldNotRetry"
      val failedState    = "Failed"

      stubFor(
        get(urlPathEqualTo("/crdl-cache/lists/BC66"))
          .inScenario(shouldNotRetry)
          .willReturn(badRequest())
          .willSetStateTo(failedState)
      )

      stubFor(
        get(urlPathEqualTo("/crdl-cache/lists/BC66"))
          .inScenario(shouldNotRetry)
          .whenScenarioStateIs(failedState)
          .willReturn(
            ok()
              .withHeader(HeaderNames.CONTENT_TYPE, ContentTypes.JSON)
              .withBody(Json.stringify(Json.toJson(exciseProductCategories)))
          )
      )

      recoverToSucceededIf[UpstreamErrorResponse] {
        connector.fetchCodeList(CodeListCode.BC66, filterKeys = None, filterProperties = None)
      }
    }
  }
}
