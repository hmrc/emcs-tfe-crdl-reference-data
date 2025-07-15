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

package uk.gov.hmrc.emcstfereferencedata.connector.crdl

import com.typesafe.config.Config
import org.apache.pekko.actor.ActorSystem
import uk.gov.hmrc.emcstfereferencedata.config.AppConfig
import uk.gov.hmrc.emcstfereferencedata.models.crdl.{CodeListCode, CrdlCodeListEntry}
import uk.gov.hmrc.http.*
import uk.gov.hmrc.http.HttpReads.Implicits.*
import uk.gov.hmrc.http.UpstreamErrorResponse.{Upstream4xxResponse, Upstream5xxResponse}
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class CrdlConnector @Inject() (config: AppConfig, httpClient: HttpClientV2)(using
  system: ActorSystem
) extends Retries {
  override protected def actorSystem: ActorSystem = system
  override protected def configuration: Config    = config.config.underlying

  private val throwOnFailureReads = throwOnFailure(readEitherOf[List[CrdlCodeListEntry]])
  private val crdlCacheUrl        = url"${config.crdlCacheUrl}/${config.crdlCachePath.split('/')}"

  def fetchCodeList(
    code: CodeListCode
  )(using ec: ExecutionContext): Future[List[CrdlCodeListEntry]] = {
    retryFor(s"fetch of codelist entries for ${code.value}") {
      // No point in retrying if our request is wrong
      case Upstream4xxResponse(_) => false
      // Attempt to recover from intermittent connectivity issues
      case Upstream5xxResponse(_) => true
    } {
      httpClient
        .get(url"$crdlCacheUrl/${code.value}")(HeaderCarrier())
        .execute[List[CrdlCodeListEntry]](using throwOnFailureReads, ec)
    }
  }
}
