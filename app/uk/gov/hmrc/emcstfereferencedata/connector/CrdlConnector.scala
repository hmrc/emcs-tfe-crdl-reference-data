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

import com.typesafe.config.Config
import org.apache.pekko.actor.ActorSystem
import play.api.Logging
import uk.gov.hmrc.emcstfereferencedata.config.AppConfig
import uk.gov.hmrc.emcstfereferencedata.models.crdl.{CodeListCode, CrdlCodeListEntry}
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.UpstreamErrorResponse.{Upstream4xxResponse, Upstream5xxResponse}
import uk.gov.hmrc.http.client.HttpClientV2

import java.net.URL
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CrdlConnector @Inject() (config: AppConfig, httpClient: HttpClientV2)(using
  system: ActorSystem
) extends Retries with Logging {
  override protected def actorSystem: ActorSystem = system

  override protected def configuration: Config = config.config.underlying

  private val throwOnFailureReads = throwOnFailure(readEitherOf[List[CrdlCodeListEntry]])
  private val crdlCacheUrl        = url"${config.crdlCacheUrl}/${config.crdlCachePath.split('/')}"

  private def urlFor(
    code: CodeListCode,
    filterKeys: Option[Set[String]],
    filterProperties: Option[Map[String, Any]]
  ): URL =
    (filterKeys, filterProperties) match {
      case (Some(keys), Some(properties)) =>
        url"$crdlCacheUrl/${code.value}?keys=${keys.mkString(",")}&$properties"
      case (Some(keys), None) =>
        url"$crdlCacheUrl/${code.value}?keys=${keys.mkString(",")}"
      case (None, Some(properties)) =>
        url"$crdlCacheUrl/${code.value}?$properties"
      case (None, None) =>
        url"$crdlCacheUrl/${code.value}"
    }

  def fetchCodeList(
    code: CodeListCode,
    filterKeys: Option[Set[String]],
    filterProperties: Option[Map[String, Any]]
  )(using hc: HeaderCarrier, ec: ExecutionContext): Future[List[CrdlCodeListEntry]] = {
    // Use the internal-auth token to call the crdl-cache service
    val hcWithInternalAuth = hc.copy(authorization = Some(Authorization(config.internalAuthToken)))
    logger.info(s"Fetching ${code.value} codelist")
    val fetchResult = retryFor(s"fetch of codelist entries for ${code.value}") {
      // No point in retrying if our request is wrong
      case Upstream4xxResponse(_) => false
      // Attempt to recover from intermittent connectivity issues
      case Upstream5xxResponse(_) => true
    } {
      httpClient
        .get(urlFor(code, filterKeys, filterProperties))(using hcWithInternalAuth)
        .execute[List[CrdlCodeListEntry]](using throwOnFailureReads)
    }
    fetchResult.failed.foreach(err => logger.error(s"Retries exceeded while fetching ${code.value} ", err))
    fetchResult
  }
}
