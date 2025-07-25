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
import uk.gov.hmrc.emcstfereferencedata.connector.crdl.CrdlConnector
import uk.gov.hmrc.emcstfereferencedata.models.crdl.CodeListCode
import uk.gov.hmrc.emcstfereferencedata.models.crdl.CodeListCode.BC17
import uk.gov.hmrc.emcstfereferencedata.models.response.ErrorResponse
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class RetrievePackagingTypesConnector @Inject() (crdlConnector: CrdlConnector) {

  lazy val logger: Logger = Logger(this.getClass)

  def retrievePackagingTypes(
    packagingTypeCodes: Option[Set[String]],
    isCountable: Option[Boolean]
  )(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Either[ErrorResponse, Map[String, String]]] = {

    crdlConnector
      .fetchCodeList(
        BC17,
        filterKeys = packagingTypeCodes,
        isCountable.map(countable => Map("countableFlag" -> countable))
      )
      .map { codeListEntries =>
        val mappedEntries: Map[String, String] =
          codeListEntries.map(entry => entry.key -> entry.value).toMap
        Right(mappedEntries)
      }
      .recover { case NonFatal(ex) =>
        logger.warn(
          s"[RetrievePackagingTypesConnector][retrievePackagingTypes] Failed response from crdl-cache",
          ex
        )
        Left(ErrorResponse.UnexpectedDownstreamResponseError)
      }
  }
}
