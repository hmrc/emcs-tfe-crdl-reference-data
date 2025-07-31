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

package uk.gov.hmrc.emcstfereferencedata.mocks.connectors

import org.scalamock.handlers.CallHandler4
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import uk.gov.hmrc.emcstfereferencedata.services.RetrievePackagingTypesService
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait MockRetrievePackagingTypesConnector extends MockFactory { this: TestSuite =>
  lazy val mockConnector: RetrievePackagingTypesService = mock[RetrievePackagingTypesService]

  object MockConnector {
    def retrievePackagingTypes(packagingTypeCodes: Option[Set[String]], isCountable: Option[Boolean])(response: Future[Map[String, String]]): CallHandler4[Option[Set[String]], Option[Boolean], HeaderCarrier, ExecutionContext, Future[Map[String, String]]] =
      (mockConnector.retrievePackagingTypes(_: Option[Set[String]],_: Option[Boolean])(_: HeaderCarrier, _: ExecutionContext)).expects(packagingTypeCodes, isCountable, *, *).returns(response)
  }

}
