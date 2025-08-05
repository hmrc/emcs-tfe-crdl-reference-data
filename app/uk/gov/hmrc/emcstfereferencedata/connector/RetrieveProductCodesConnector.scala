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

import play.api.Logger
import uk.gov.hmrc.emcstfereferencedata.models.request.CnInformationRequest
import uk.gov.hmrc.emcstfereferencedata.models.response.{CnCodeInformation, ErrorResponse}
import uk.gov.hmrc.emcstfereferencedata.repositories.ExciseProductsRepository
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class RetrieveProductCodesConnector @Inject() (repository: ExciseProductsRepository) {

  lazy val logger: Logger = Logger(this.getClass)

  def retrieveProductCodes(cnInformationRequest: CnInformationRequest)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Either[ErrorResponse, Map[String, CnCodeInformation]]] =

    repository
      .fetchProductCodesInformation(cnInformationRequest)
      .map(Right(_))
      .recover {
        case NonFatal(exception) => {
          logger.warn(
            s"[RetrieveProductCodesConnector][retrieveProductCodes] Unexpected Error fetching data from repository,",
            exception
          )
          Left(ErrorResponse.UnexpectedDownstreamResponseError)
        }
      }
}
