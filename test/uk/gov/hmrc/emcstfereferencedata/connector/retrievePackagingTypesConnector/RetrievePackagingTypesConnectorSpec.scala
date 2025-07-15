package uk.gov.hmrc.emcstfereferencedata.connector.retrievePackagingTypesConnector

import org.scalatest.concurrent.ScalaFutures
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.mockito.Mockito.*
import org.scalatestplus.mockito.MockitoSugar

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.emcstfereferencedata.config.AppConfig
import uk.gov.hmrc.emcstfereferencedata.models.crdl.{CodeListCode, CrdlCodeListEntry}
import uk.gov.hmrc.emcstfereferencedata.models.response.{ErrorResponse, PackagingType}
import play.api.libs.json.Json
import uk.gov.hmrc.emcstfereferencedata.connector.crdl.CrdlConnector
import uk.gov.hmrc.emcstfereferencedata.connector.retrievePackagingTypes.RetrievePackagingTypesConnectorCRDL
import uk.gov.hmrc.emcstfereferencedata.models.crdl.CodeListCode.BC17

class RetrievePackagingTypesConnectorCRDLSpec
  extends AnyWordSpec
  with Matchers
  with ScalaFutures
  with MockitoSugar {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val httpClient: HttpClientV2 = mock[HttpClientV2]
  val config: AppConfig = mock[AppConfig]
  val crdlConnector: CrdlConnector = mock[CrdlConnector]

  val connector = new RetrievePackagingTypesConnectorCRDL(httpClient, config, crdlConnector)

  "RetrievePackagingTypesConnectorCRDL" should {

    "return a Right(Map) when CrdlConnector returns valid entries" in {


      val crdlEntries = List(
        CrdlCodeListEntry(
          key = "1A",
          value = "Drum, steel",
          properties = Json.obj("countableFlag" -> true)
        ),
        CrdlCodeListEntry(
          key = "1B",
          value = "Drum, aluminium",
          properties = Json.obj("countableFlag" -> false)
        )
      )

      when(
        crdlConnector.fetchCodeList(BC17)
      ).thenReturn(Future.successful(crdlEntries))

      val result = connector.retrievePackagingTypes().futureValue

      result shouldBe Right(
        Map(
          "1A" -> PackagingType("1A", "Drum, steel", isCountable = true),
          "1B" -> PackagingType("1B", "Drum, aluminium", isCountable = false)
        )
      )
    }

    "return a Left(ErrorResponse) when parsing fails" in {

      val badEntry = CrdlCodeListEntry(
        key = "BAD",
        value = "BadEntry",
        properties = Json.obj() // missing "countableFlag"
      )

      when(crdlConnector.fetchCodeList(BC17))
        .thenReturn(Future.successful(List(badEntry)))

      val result = connector.retrievePackagingTypes().futureValue

      result shouldBe Left(ErrorResponse.JsonValidationError)
    }
  }
}
