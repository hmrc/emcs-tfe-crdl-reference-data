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

import org.scalamock.handlers.CallHandler2
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import uk.gov.hmrc.emcstfereferencedata.connector.retrieveOtherReferenceData.RetrieveOtherReferenceDataConnector
import uk.gov.hmrc.emcstfereferencedata.models.response.ErrorResponse
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait MockRetrieveOtherReferenceDataConnector extends MockFactory { this: TestSuite =>
  lazy val mockConnector: RetrieveOtherReferenceDataConnector = mock[RetrieveOtherReferenceDataConnector]

  object MockConnector {
    def retrieveWineOperations()(response: Future[Either[ErrorResponse, Map[String, String]]]): CallHandler2[HeaderCarrier, ExecutionContext, Future[Either[ErrorResponse, Map[String, String]]]] =
      (mockConnector.retrieveWineOperations()(_: HeaderCarrier, _: ExecutionContext)).expects(*, *).returns(response)

    def retrieveMemberStates()(response: Future[Either[ErrorResponse, Map[String, String]]]): CallHandler2[HeaderCarrier, ExecutionContext, Future[Either[ErrorResponse, Map[String, String]]]] =
      (mockConnector.retrieveMemberStates()(_: HeaderCarrier, _: ExecutionContext)).expects(*, *).returns(response)

    def retrieveCountries()(response: Future[Either[ErrorResponse, Map[String, String]]]): CallHandler2[HeaderCarrier, ExecutionContext, Future[Either[ErrorResponse, Map[String, String]]]] =
      (mockConnector.retrieveCountries()(_: HeaderCarrier, _: ExecutionContext)).expects(*, *).returns(response)

    def retrieveTransportUnits()(response: Future[Either[ErrorResponse, Map[String, String]]]): CallHandler2[HeaderCarrier, ExecutionContext, Future[Either[ErrorResponse, Map[String, String]]]] =
      (mockConnector.retrieveTransportUnits()(_: HeaderCarrier, _: ExecutionContext)).expects(*, *).returns(response)

    def retrieveTypesOfDocument()(response: Future[Either[ErrorResponse, Map[String, String]]]): CallHandler2[HeaderCarrier, ExecutionContext, Future[Either[ErrorResponse, Map[String, String]]]] =
      (mockConnector.retrieveTypesOfDocument()(_: HeaderCarrier, _: ExecutionContext)).expects(*, *).returns(response)
  }

}
