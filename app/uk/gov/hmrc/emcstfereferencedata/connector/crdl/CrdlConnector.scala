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
