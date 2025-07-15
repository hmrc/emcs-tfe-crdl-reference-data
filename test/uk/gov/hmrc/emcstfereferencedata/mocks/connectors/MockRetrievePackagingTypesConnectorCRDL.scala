package uk.gov.hmrc.emcstfereferencedata.mocks.connectors

import org.scalamock.handlers.CallHandler2
import org.scalamock.scalatest.MockFactory
import org.scalatest.TestSuite
import uk.gov.hmrc.emcstfereferencedata.connector.retrievePackagingTypes.{RetrievePackagingTypesConnector, RetrievePackagingTypesConnectorCRDL}
import uk.gov.hmrc.emcstfereferencedata.models.response.{ErrorResponse, PackagingType}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.{ExecutionContext, Future}

trait MockRetrievePackagingTypesConnectorCRDL extends MockFactory { this: TestSuite =>
  lazy val mockConnector: RetrievePackagingTypesConnector = mock[RetrievePackagingTypesConnectorCRDL]

  object MockConnector {
    def retrievePackagingTypes()(response: Future[Either[ErrorResponse, Map[String, PackagingType]]]): CallHandler2[HeaderCarrier, ExecutionContext, Future[Either[ErrorResponse, Map[String, PackagingType]]]] =
      (mockConnector.retrievePackagingTypes()(_: HeaderCarrier, _: ExecutionContext)).expects(*, *).returns(response)
  }

}

