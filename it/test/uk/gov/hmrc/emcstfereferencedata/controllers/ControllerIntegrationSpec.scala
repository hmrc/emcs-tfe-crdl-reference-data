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

import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.ws.JsonBodyWritables
import uk.gov.hmrc.emcstfereferencedata.config.SchedulerModule
import uk.gov.hmrc.http.test.HttpClientV2Support
import uk.gov.hmrc.http.{HeaderCarrier, HttpReadsInstances}
import uk.gov.hmrc.http.StringContextOps

import scala.concurrent.ExecutionContext

trait ControllerIntegrationSpec
  extends AnyWordSpec
  with Matchers
  with MockitoSugar
  with ScalaFutures
  with IntegrationPatience
  with HttpClientV2Support
  with FakeAuthAction
  with GuiceOneServerPerSuite
  with BeforeAndAfterEach
  with HttpReadsInstances
  with JsonBodyWritables {

  given ExecutionContext = ExecutionContext.global
  given HeaderCarrier    = HeaderCarrier()

  // FIXME: We are maintaining the context path of the old emcs-tfe-reference-data service because it is hardcoded into the TFE frontends
  protected def baseUrl = url"http://localhost:$port/emcs-tfe-reference-data"

  override def fakeApplication(): Application = {
    GuiceApplicationBuilder()
      // Stop Quartz from complaining about being instantiated multiple times
      .disable(classOf[SchedulerModule])
      .build()
  }
}
