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

package uk.gov.hmrc.emcstfereferencedata.connector.retrieveProductCodes

import javax.inject.Inject
import play.api.Logger
import uk.gov.hmrc.emcstfereferencedata.models.request.CnInformationRequest
import uk.gov.hmrc.emcstfereferencedata.models.response.{CnCodeInformation, ErrorResponse}
import uk.gov.hmrc.emcstfereferencedata.repositories.ExciseProductsRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

class RetrieveProductCodesConnectorCRDL @Inject() (
  repository: ExciseProductsRepository
) extends RetrieveProductCodesConnector {

  lazy val logger: Logger = Logger(this.getClass)

  override def retrieveProductCodes(cnInformationRequest: CnInformationRequest)(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Either[ErrorResponse, Map[String, CnCodeInformation]]] =

    repository
      .fetchProductCodesInformation(cnInformationRequest)
      .map(Right(_))
      .recover {
        case exception: Exception => {
          logger.warn(
            s"[RetrieveProductCodesConnectorCRDL][retrieveProductCodes] Unexpected Error fetching data from repository,",
            exception
          )
          Left(ErrorResponse.UnexpectedDownstreamResponseError)
        }
      }
}
