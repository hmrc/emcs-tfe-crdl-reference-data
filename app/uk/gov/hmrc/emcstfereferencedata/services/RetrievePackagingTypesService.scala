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

import uk.gov.hmrc.emcstfereferencedata.connector.retrievePackagingTypes.RetrievePackagingTypesConnector
import uk.gov.hmrc.emcstfereferencedata.models.response.ErrorResponse
import uk.gov.hmrc.emcstfereferencedata.models.response.ErrorResponse.NoDataReturnedFromDatabaseError
import uk.gov.hmrc.emcstfereferencedata.utils.Logging
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.collection.Map
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RetrievePackagingTypesService @Inject()(connector: RetrievePackagingTypesConnector) extends Logging {

  def retrievePackagingTypes(packagingTypesList: Set[String])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[ErrorResponse, Map[String, String]]] = {
    connector.retrievePackagingTypes(Some(packagingTypesList), isCountable = None)
      .map(
        _.flatMap { packagingTypes =>
          if (packagingTypes.nonEmpty) {
            Right(packagingTypes.view.mapValues(_.description).toMap)
          }
          else {
            logger.warn(s"No data returned for input packaging types: $packagingTypesList")
            Left(NoDataReturnedFromDatabaseError)
          }
        }
      )
  }

  def retrievePackagingTypes(isCountable: Option[Boolean])(implicit hc: HeaderCarrier, ec: ExecutionContext): Future[Either[ErrorResponse, Map[String, String]]] = {
    connector.retrievePackagingTypes(packagingTypeCodes = None, isCountable)
      .map {
        _.flatMap { packagingTypes =>
          if (packagingTypes.nonEmpty) {
            Right(packagingTypes.view.mapValues(_.description).toMap)
          }
          else {
            logger.warn("No data returned for all packaging types")
            Left(NoDataReturnedFromDatabaseError)
          }
        }
      }
  }

}
