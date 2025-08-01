/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.emcstfereferencedata.support

import org.scalamock.scalatest.MockFactory
import org.scalatest.EitherValues
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.DefaultAwaitTimeout
import play.api.test.FutureAwaits
import uk.gov.hmrc.emcstfereferencedata.config.SchedulerModule
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

trait UnitSpec
  extends AnyWordSpecLike
  with MockFactory
  with EitherValues
  with Matchers
  with FutureAwaits
  with DefaultAwaitTimeout
  with GuiceOneAppPerSuite {
  implicit lazy val hc: HeaderCarrier    = HeaderCarrier()
  implicit lazy val ec: ExecutionContext = ExecutionContext.global

  override def fakeApplication(): Application = {
    GuiceApplicationBuilder()
      // Stop Quartz from complaining about being instantiated multiple times
      .disable(classOf[SchedulerModule])
      .build()
  }
}
