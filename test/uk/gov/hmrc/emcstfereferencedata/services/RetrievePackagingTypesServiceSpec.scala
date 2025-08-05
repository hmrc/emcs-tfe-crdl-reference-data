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

package uk.gov.hmrc.emcstfereferencedata.services

import uk.gov.hmrc.emcstfereferencedata.fixtures.PackagingTypeFixtures
import uk.gov.hmrc.emcstfereferencedata.mocks.connectors.MockRetrievePackagingTypesConnector
import uk.gov.hmrc.emcstfereferencedata.models.response.ErrorResponse.{
  NoDataReturnedFromDatabaseError,
  UnexpectedDownstreamResponseError
}
import uk.gov.hmrc.emcstfereferencedata.support.UnitSpec

import scala.concurrent.Future

class RetrievePackagingTypesServiceSpec
  extends UnitSpec
  with MockRetrievePackagingTypesConnector
  with PackagingTypeFixtures {

  object TestService extends RetrievePackagingTypesService(mockConnector)

  "The RetrievePackagingTypesService" should {
    "return a successful response containing the PackagingTypes" when {
      "retrievePackagingTypes is called with packaging type codes" in {
        MockConnector.retrievePackagingTypes(
          packagingTypeCodes = Some(testPackagingTypes),
          isCountable = None
        )(Future.successful(Right(testPackagingTypesConnectorResult)))

        await(
          TestService.retrievePackagingTypes(
            packagingTypeCodes = Some(testPackagingTypes),
            isCountable = None
          )
        ) shouldBe Right(testPackagingTypesConnectorResult)
      }

      "retrievePackagingTypes is called with no codes for countable packaging types" in {
        MockConnector.retrievePackagingTypes(packagingTypeCodes = None, isCountable = Some(true))(
          Future.successful(Right(testPackagingTypesConnectorResult))
        )

        await(
          TestService.retrievePackagingTypes(packagingTypeCodes = None, isCountable = Some(true))
        ) shouldBe Right(testPackagingTypesConnectorResult)
      }

      "retrievePackagingTypes is called with no codes for non-countable packaging types" in {
        MockConnector.retrievePackagingTypes(packagingTypeCodes = None, isCountable = Some(false))(
          Future.successful(Right(testPackagingTypesConnectorResult))
        )

        await(
          TestService.retrievePackagingTypes(packagingTypeCodes = None, isCountable = Some(false))
        ) shouldBe Right(testPackagingTypesConnectorResult)
      }

      "retrievePackagingTypes is called with no codes for all packaging types" in {
        MockConnector.retrievePackagingTypes(packagingTypeCodes = None, isCountable = None)(
          Future.successful(Right(testPackagingTypesConnectorResult))
        )

        await(
          TestService.retrievePackagingTypes(packagingTypeCodes = None, isCountable = None)
        ) shouldBe Right(testPackagingTypesConnectorResult)
      }
    }

    "return an Error Response" when {
      "there is no data available" in {
        MockConnector.retrievePackagingTypes(
          packagingTypeCodes = Some(testPackagingTypes),
          isCountable = None
        )(Future.successful(Right(Map.empty)))

        await(
          TestService.retrievePackagingTypes(
            packagingTypeCodes = Some(testPackagingTypes),
            isCountable = None
          )
        ) shouldBe Left(NoDataReturnedFromDatabaseError)
      }

      "there is an upstream error for retrievePackagingTypes(Seq[String]) method call" in {
        MockConnector.retrievePackagingTypes(
          packagingTypeCodes = Some(testPackagingTypes),
          isCountable = None
        )(Future.successful(Left(UnexpectedDownstreamResponseError)))

        await(
          TestService.retrievePackagingTypes(
            packagingTypeCodes = Some(testPackagingTypes),
            isCountable = None
          )
        ) shouldBe Left(UnexpectedDownstreamResponseError)
      }

      "the connector throws an exception for retrievePackagingTypes(Seq[String]) method call" in {
        MockConnector.retrievePackagingTypes(
          packagingTypeCodes = Some(testPackagingTypes),
          isCountable = None
        )(Future.failed(new RuntimeException("Error")))

        assertThrows[RuntimeException] {
          await(
            TestService.retrievePackagingTypes(
              packagingTypeCodes = Some(testPackagingTypes),
              isCountable = None
            )
          )
        }
      }
    }
  }

}
