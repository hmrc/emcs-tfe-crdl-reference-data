package uk.gov.hmrc.emcstfereferencedata.connector.retrieveAllEPCCodes

import uk.gov.hmrc.emcstfereferencedata.models.response.{ErrorResponse, ExciseProductCode}
import uk.gov.hmrc.emcstfereferencedata.repositories.ExciseProductsRepository
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class RetrieveAllEPCCodesConnectorCRDL @Inject() (
  repository: ExciseProductsRepository
) extends RetrieveAllEPCCodesConnector {

  override def retrieveAllEPCCodes()(implicit
    ec: ExecutionContext,
    hc: HeaderCarrier
  ): Future[Either[ErrorResponse, Seq[ExciseProductCode]]] = {
    repository
      .fetchAllEPCCodes()
      .map(Right())
      .recover({
        case ex: Exception => {
          logger.warn(
            "[RetrieveAllEPCCodesConnectorCRDL][retrieveAllEPCCodes] Unexpected Error fetching data from repository",
            ex
          )
        }
      })

  }
}
