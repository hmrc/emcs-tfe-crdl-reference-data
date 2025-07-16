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

package uk.gov.hmrc.emcstfereferencedata.connector.retrievePackagingTypes

import play.api.Logger
import play.api.libs.json.{JsValue, Reads}
import uk.gov.hmrc.emcstfereferencedata.config.AppConfig
import uk.gov.hmrc.emcstfereferencedata.connector.crdl.CrdlConnector
import uk.gov.hmrc.emcstfereferencedata.models.crdl.CodeListCode
import uk.gov.hmrc.emcstfereferencedata.models.crdl.CodeListCode.BC17
import uk.gov.hmrc.emcstfereferencedata.models.response.{ErrorResponse, PackagingType}
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.HeaderCarrier
import play.api.libs.json.{JsError, JsSuccess}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RetrievePackagingTypesConnectorCRDL @Inject() (
  val http: HttpClientV2,
  val config: AppConfig,
  val crdlConnector: CrdlConnector
) extends RetrievePackagingTypesConnector {

  lazy val logger: Logger = Logger(this.getClass)

  override def retrievePackagingTypes()(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Either[ErrorResponse, Map[String, PackagingType]]] = {

    crdlConnector.fetchCodeList(BC17).map { entries =>
      val results: List[Either[ErrorResponse, (String, PackagingType)]] = entries.map { entry =>
        (entry.properties \ "countableFlag").validate[Boolean] match {
          case JsSuccess(flag, _) =>
            val packagingType = (PackagingType(entry.key, entry.value, flag))
            Right(entry.key -> packagingType)
          case JsError(errors) => {
            logger.warn(
              s"[RetrievePackagingTypesConnectorCRDL][retrievePackagingTypes] Failed to Parse PackagingType ${entry.value} with property object ${entry.properties}, errors: ${errors}"
            )
            Left(ErrorResponse.JsonValidationError)
          }
        }
      }

      val (failures, successes) = results.partitionMap(identity)

      if (failures.nonEmpty) {

        Left(failures.head)
      } else {
        Right(successes.toMap)
      }
    }
  }.recover { case ex: Exception =>
    logger.warn(
      s"[RetrievePackagingTypesConnectorCRDL][retrievePackagingTypes] Failed response from crdl-cache",
      ex
    )
    Left(ErrorResponse.UnexpectedDownstreamResponseError)
  }
}
