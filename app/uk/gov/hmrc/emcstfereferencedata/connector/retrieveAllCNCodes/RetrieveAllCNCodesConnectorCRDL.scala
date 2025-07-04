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

package uk.gov.hmrc.emcstfereferencedata.connector.retrieveAllCNCodes

import uk.gov.hmrc.emcstfereferencedata.models.response.{CnCodeInformation, ErrorResponse}
import uk.gov.hmrc.emcstfereferencedata.repositories.CnCodesRepository
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RetrieveAllCNCodesConnectorCRDL @Inject() (
  repository: CnCodesRepository
) extends RetrieveAllCNCodesConnector {

  override def retrieveAllCnCodes(
    exciseProductCode: String
  )(using
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): Future[Either[ErrorResponse, Seq[CnCodeInformation]]] = {

    repository
      .fetchCnCodesForProduct(exciseProductCode)
      .map(Right(_))
  }

}
