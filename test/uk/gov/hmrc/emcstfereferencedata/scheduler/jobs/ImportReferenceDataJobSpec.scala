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

package uk.gov.hmrc.emcstfereferencedata.scheduler.jobs

import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.stream.scaladsl.{Sink, Source}
import org.mockito.ArgumentMatchers.{any, eq as equalTo}
import org.mockito.Mockito.*
import org.mongodb.scala.{ClientSession, MongoClient, MongoDatabase, SingleObservable}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.mockito.MockitoSugar
import play.api.http.Status
import play.api.libs.json.Json
import uk.gov.hmrc.emcstfereferencedata.config.AppConfig
import uk.gov.hmrc.emcstfereferencedata.connector.CrdlConnector
import uk.gov.hmrc.emcstfereferencedata.models.crdl.{CodeListCode, CrdlCodeListEntry}
import uk.gov.hmrc.emcstfereferencedata.models.errors.MongoError
import uk.gov.hmrc.emcstfereferencedata.models.response.{CnCodeInformation, ExciseProductCode}
import uk.gov.hmrc.emcstfereferencedata.repositories.{CnCodesRepository, CodeListsRepository, ExciseProductsRepository}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.lock.{Lock, MongoLockRepository}

import scala.concurrent.{ExecutionContext, Future}

class ImportReferenceDataJobSpec
  extends AnyFlatSpec
  with Matchers
  with MockitoSugar
  with ScalaFutures
  with IntegrationPatience
  with BeforeAndAfterEach {

  private val mongoComponent           = mock[MongoComponent]
  private val mongoClient              = mock[MongoClient]
  private val mongoDatabase            = mock[MongoDatabase]
  private val clientSession            = mock[ClientSession]
  private val lockRepository           = mock[MongoLockRepository]
  private val codeListsRepository      = mock[CodeListsRepository]
  private val cnCodesRepository        = mock[CnCodesRepository]
  private val exciseProductsRepository = mock[ExciseProductsRepository]
  private val crdlConnector            = mock[CrdlConnector]
  private val appConfig                = mock[AppConfig]

  given ActorSystem      = ActorSystem("test")
  given ExecutionContext = ExecutionContext.global

  private val refDataJob = new ImportReferenceDataJob(
    mongoComponent,
    lockRepository,
    crdlConnector,
    codeListsRepository,
    cnCodesRepository,
    exciseProductsRepository
  )

  private val BC36Entries = List(
    CrdlCodeListEntry(
      "B000",
      "Beer",
      Json.obj(
        "unitOfMeasureCode"                  -> "3",
        "degreePlatoApplicabilityFlag"       -> true,
        "actionIdentification"               -> "1090",
        "exciseProductsCategoryCode"         -> "B",
        "alcoholicStrengthApplicabilityFlag" -> true,
        "densityApplicabilityFlag"           -> false
      )
    ),
    CrdlCodeListEntry(
      "I000",
      "Intermediate products",
      Json.obj(
        "unitOfMeasureCode"                  -> "3",
        "degreePlatoApplicabilityFlag"       -> true,
        "actionIdentification"               -> "1109",
        "exciseProductsCategoryCode"         -> "I",
        "alcoholicStrengthApplicabilityFlag" -> true,
        "densityApplicabilityFlag"           -> false
      )
    )
  )

  private val BC37Entries = List(
    CrdlCodeListEntry(
      "22060059",
      "Other still fermented beverages in containers holding 2 litres or less",
      Json.obj("actionIdentification" -> "323")
    )
  )

  private val BC66Entries = List(
    CrdlCodeListEntry(
      "B",
      "Beer",
      Json.obj("actionIdentification" -> "1084")
    ),
    CrdlCodeListEntry(
      "I",
      "Intermediate products",
      Json.obj("actionIdentification" -> "1086")
    )
  )

  private val E200Entries = Seq(
    CrdlCodeListEntry(
      "22060059",
      "B000",
      Json.obj("actionIdentification" -> "355")
    ),
    CrdlCodeListEntry(
      "22060059",
      "I000",
      Json.obj("actionIdentification" -> "526")
    )
  )

  private val HMRCBC36Entries = List(
    CrdlCodeListEntry(
      "V000",
      "Vaping",
      Json.obj(
        "unitOfMeasureCode"                  -> "2",
        "degreePlatoApplicabilityFlag"       -> false,
        "actionIdentification"               -> "352",
        "exciseProductsCategoryCode"         -> "V",
        "alcoholicStrengthApplicabilityFlag" -> false,
        "densityApplicabilityFlag"           -> true
      )
    )
  )

  private val HMRCBC37Entries = List(
    CrdlCodeListEntry(
      "15200000",
      "Vegetable Glycerine with a purity of less than 95%",
      Json.obj("actionIdentification" -> "17")
    ),
    CrdlCodeListEntry(
      "24041200",
      "Nicotine-containing vape liquids, including cartridges, refills, and standalone liquids (not containing tobacco)",
      Json.obj("actionIdentification" -> "17")
    ),
    CrdlCodeListEntry(
      "24041990",
      "Zero nicotine liquid in any container size",
      Json.obj("actionIdentification" -> "17")
    ),
    CrdlCodeListEntry(
      "29053200",
      "Propylene glycol (propane-1.2-diol)",
      Json.obj("actionIdentification" -> "17")
    ),
    CrdlCodeListEntry(
      "29054500",
      "Vegetable Glycerine with a purity of 95% or more",
      Json.obj("actionIdentification" -> "17")
    ),
    CrdlCodeListEntry(
      "29397910",
      "Nicotine intended for vaping",
      Json.obj("actionIdentification" -> "17")
    ),
    CrdlCodeListEntry(
      "85434000",
      "Electronic device for vaping",
      Json.obj("actionIdentification" -> "17")
    )
  )

  private val HMRCBC66Entries = List(
    CrdlCodeListEntry(
      "V",
      "Vaping Products",
      Json.obj("actionIdentification" -> "1081")
    )
  )

  private val HMRCE200Entries = Seq(
    CrdlCodeListEntry(
      "15200000",
      "V000",
      Json.obj("actionIdentification" -> "632")
    ),
    CrdlCodeListEntry(
      "24041200",
      "V000",
      Json.obj("actionIdentification" -> "632")
    ),
    CrdlCodeListEntry(
      "24041990",
      "V000",
      Json.obj("actionIdentification" -> "632")
    ),
    CrdlCodeListEntry(
      "29053200",
      "V000",
      Json.obj("actionIdentification" -> "632")
    ),
    CrdlCodeListEntry(
      "29054500",
      "V000",
      Json.obj("actionIdentification" -> "632")
    ),
    CrdlCodeListEntry(
      "29397910",
      "V000",
      Json.obj("actionIdentification" -> "632")
    ),
    CrdlCodeListEntry(
      "85434000",
      "V000",
      Json.obj("actionIdentification" -> "632")
    )
  )

  private val CnCodes = List(
    CnCodeInformation(
      "22060059",
      "Other still fermented beverages in containers holding 2 litres or less",
      "B000",
      "Beer",
      unitOfMeasureCode = 3
    ),
    CnCodeInformation(
      "22060059",
      "Other still fermented beverages in containers holding 2 litres or less",
      "I000",
      "Intermediate products",
      unitOfMeasureCode = 3
    )
  )

  private val ExciseProducts = List(
    ExciseProductCode(
      "B000",
      "Beer",
      "B",
      "Beer",
      unitOfMeasureCode = 3
    ),
    ExciseProductCode(
      "I000",
      "Intermediate products",
      "I",
      "Intermediate products",
      unitOfMeasureCode = 3
    )
  )

  override def beforeEach(): Unit = {
    reset(
      mongoComponent,
      mongoClient,
      mongoDatabase,
      clientSession,
      lockRepository,
      cnCodesRepository,
      exciseProductsRepository,
      codeListsRepository,
      crdlConnector,
      appConfig
    )

    // Job lock
    val mockLock = mock[Lock]
    when(lockRepository.takeLock(any(), any(), any())).thenReturn(Future.successful(Some(mockLock)))
    when(lockRepository.releaseLock(any(), any())).thenReturn(Future.unit)

    // Transactions
    when(mongoComponent.client).thenReturn(mongoClient)
    when(mongoComponent.database).thenReturn(mongoDatabase)
    when(mongoClient.startSession(any())).thenReturn(SingleObservable(clientSession))
    when(clientSession.commitTransaction())
      .thenAnswer(_ => Source.empty[Void].runWith(Sink.asPublisher(fanout = false)))
    when(clientSession.abortTransaction())
      .thenAnswer(_ => Source.empty[Void].runWith(Sink.asPublisher(fanout = false)))
  }

  "ImportReferenceDataJob.importReferenceData" should "import the configured codelists and build the derived domain objects" in {
    // CRDL connector responses
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("BC36")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(BC36Entries))
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("BC37")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(BC37Entries))
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("BC66")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(BC66Entries))
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("E200")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(E200Entries))

    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("HMRCBC36")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(HMRCBC36Entries))
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("HMRCBC37")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(HMRCBC37Entries))
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("HMRCBC66")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(HMRCBC66Entries))
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("HMRCE200")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(HMRCE200Entries))

    // Mongo collection manipulation
    when(codeListsRepository.saveCodeListEntries(equalTo(clientSession), any()))
      .thenReturn(Future.unit)
    when(codeListsRepository.buildCnCodes(equalTo(clientSession), any()))
      .thenReturn(Future.successful(CnCodes))
    when(codeListsRepository.buildExciseProducts(equalTo(clientSession), any()))
      .thenReturn(Future.successful(ExciseProducts))
    when(exciseProductsRepository.replaceExciseProducts(equalTo(clientSession), any()))
      .thenReturn(Future.unit)
    when(cnCodesRepository.replaceCnCodes(equalTo(clientSession), any())).thenReturn(Future.unit)

    refDataJob.importReferenceData().futureValue

    verify(codeListsRepository, times(4)).saveCodeListEntries(equalTo(clientSession), any())
    verify(cnCodesRepository, times(1)).replaceCnCodes(equalTo(clientSession), any())
    verify(exciseProductsRepository, times(1)).replaceExciseProducts(equalTo(clientSession), any())

    verify(clientSession, times(1)).commitTransaction()
  }

  it should "roll back and fail to build excise products when there is an issue fetching one of the required codelists" in {
    // CRDL connector responses
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("BC36")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(BC36Entries))
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("BC37")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(BC37Entries))
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("BC66")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.failed(UpstreamErrorResponse("Boom!", Status.INTERNAL_SERVER_ERROR)))
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("E200")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(E200Entries))

    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("HMRCBC36")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(HMRCBC36Entries))
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("HMRCBC37")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(HMRCBC37Entries))
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("HMRCBC66")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(HMRCBC66Entries))
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("HMRCE200")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(HMRCE200Entries))

    // Mongo collection manipulation
    when(codeListsRepository.saveCodeListEntries(equalTo(clientSession), any()))
      .thenReturn(Future.unit)
    when(codeListsRepository.buildCnCodes(equalTo(clientSession), any()))
      .thenReturn(Future.successful(CnCodes))
    when(codeListsRepository.buildExciseProducts(equalTo(clientSession), any()))
      .thenReturn(Future.successful(ExciseProducts))
    when(exciseProductsRepository.replaceExciseProducts(equalTo(clientSession), any()))
      .thenReturn(Future.unit)
    when(cnCodesRepository.replaceCnCodes(equalTo(clientSession), any())).thenReturn(Future.unit)

    refDataJob.importReferenceData().failed.futureValue shouldBe an[UpstreamErrorResponse]

    verify(codeListsRepository, atLeastOnce()).saveCodeListEntries(
      equalTo(clientSession),
      any()
    )
    verify(cnCodesRepository, times(1)).replaceCnCodes(equalTo(clientSession), any())
    verify(exciseProductsRepository, never()).replaceExciseProducts(equalTo(clientSession), any())

    verify(clientSession, times(1)).abortTransaction()
  }

  it should "roll back and fail to build CN codes when there is an issue fetching one of the required codelists" in {
    // CRDL connector responses
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("BC36")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(BC36Entries))
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("BC37")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(BC37Entries))
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("BC66")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(BC66Entries))
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("E200")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.failed(UpstreamErrorResponse("Boom!", Status.INTERNAL_SERVER_ERROR)))

    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("HMRCBC36")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(HMRCBC36Entries))
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("HMRCBC37")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(HMRCBC37Entries))
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("HMRCBC66")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(HMRCBC66Entries))
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("HMRCE200")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(HMRCE200Entries))

    // Mongo collection manipulation
    when(codeListsRepository.saveCodeListEntries(equalTo(clientSession), any()))
      .thenReturn(Future.unit)
    when(codeListsRepository.buildCnCodes(equalTo(clientSession), any()))
      .thenReturn(Future.successful(CnCodes))
    when(codeListsRepository.buildExciseProducts(equalTo(clientSession), any()))
      .thenReturn(Future.successful(ExciseProducts))
    when(exciseProductsRepository.replaceExciseProducts(equalTo(clientSession), any()))
      .thenReturn(Future.unit)
    when(cnCodesRepository.replaceCnCodes(equalTo(clientSession), any())).thenReturn(Future.unit)

    refDataJob.importReferenceData().failed.futureValue shouldBe an[UpstreamErrorResponse]

    verify(codeListsRepository, atLeastOnce()).saveCodeListEntries(
      equalTo(clientSession),
      any()
    )
    verify(cnCodesRepository, never()).replaceCnCodes(equalTo(clientSession), any())
    verify(exciseProductsRepository, never()).replaceExciseProducts(equalTo(clientSession), any())

    verify(clientSession, times(1)).abortTransaction()
  }

  it should "roll back and fail to build either domain object when there is an issue fetching the excise products codelist" in {
    // CRDL connector responses
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("BC36")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.failed(UpstreamErrorResponse("Boom!", Status.INTERNAL_SERVER_ERROR)))
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("BC37")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(BC37Entries))
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("BC66")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(BC66Entries))
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("E200")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(E200Entries))

    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("HMRCBC36")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(BC36Entries))
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("HMRCBC37")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(BC37Entries))
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("HMRCBC66")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(BC66Entries))
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("HMRCE200")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(E200Entries))

    // Mongo collection manipulation
    when(codeListsRepository.saveCodeListEntries(equalTo(clientSession), any()))
      .thenReturn(Future.unit)
    when(codeListsRepository.buildCnCodes(equalTo(clientSession), any()))
      .thenReturn(Future.successful(CnCodes))
    when(codeListsRepository.buildExciseProducts(equalTo(clientSession), any()))
      .thenReturn(Future.successful(ExciseProducts))
    when(exciseProductsRepository.replaceExciseProducts(equalTo(clientSession), any()))
      .thenReturn(Future.unit)
    when(cnCodesRepository.replaceCnCodes(equalTo(clientSession), any())).thenReturn(Future.unit)

    refDataJob.importReferenceData().failed.futureValue shouldBe an[UpstreamErrorResponse]

    verify(codeListsRepository, never()).saveCodeListEntries(equalTo(clientSession), any())
    verify(cnCodesRepository, never()).replaceCnCodes(equalTo(clientSession), any())
    verify(exciseProductsRepository, never()).replaceExciseProducts(equalTo(clientSession), any())

    verify(clientSession, times(1)).abortTransaction()
  }

  it should "roll back when there is an issue saving the excise products" in {
    // CRDL connector responses
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("BC36")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(BC36Entries))
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("BC37")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(BC37Entries))
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("BC66")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(BC66Entries))
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("E200")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(E200Entries))

    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("HMRCBC36")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(HMRCBC36Entries))
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("HMRCBC37")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(HMRCBC37Entries))
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("HMRCBC66")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(HMRCBC66Entries))
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("HMRCE200")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(HMRCE200Entries))

    // Mongo collection manipulation
    when(codeListsRepository.saveCodeListEntries(equalTo(clientSession), any()))
      .thenReturn(Future.unit)
    when(codeListsRepository.buildCnCodes(equalTo(clientSession), any()))
      .thenReturn(Future.successful(CnCodes))
    when(codeListsRepository.buildExciseProducts(equalTo(clientSession), any()))
      .thenReturn(Future.successful(ExciseProducts))
    when(exciseProductsRepository.replaceExciseProducts(equalTo(clientSession), any()))
      .thenReturn(Future.failed(MongoError.NotAcknowledged))
    when(cnCodesRepository.replaceCnCodes(equalTo(clientSession), any())).thenReturn(Future.unit)

    refDataJob.importReferenceData().failed.futureValue shouldBe a[MongoError]

    verify(codeListsRepository, atLeastOnce()).saveCodeListEntries(
      equalTo(clientSession),
      any()
    )
    verify(exciseProductsRepository, times(1)).replaceExciseProducts(equalTo(clientSession), any())

    verify(clientSession, times(1)).abortTransaction()
  }

  it should "roll back when there is an issue saving the CN codes" in {
    // CRDL connector responses
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("BC36")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(BC36Entries))
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("BC37")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(BC37Entries))
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("BC66")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(BC66Entries))
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("E200")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(E200Entries))

    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("HMRCBC36")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(HMRCBC36Entries))
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("HMRCBC37")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(HMRCBC37Entries))
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("HMRCBC66")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(HMRCBC66Entries))
    when(
      crdlConnector.fetchCodeList(CodeListCode(equalTo("HMRCE200")), equalTo(None), equalTo(None))(using
        any(),
        any()
      )
    )
      .thenReturn(Future.successful(HMRCE200Entries))

    // Mongo collection manipulation
    when(codeListsRepository.saveCodeListEntries(equalTo(clientSession), any()))
      .thenReturn(Future.unit)
    when(codeListsRepository.buildCnCodes(equalTo(clientSession), any()))
      .thenReturn(Future.successful(CnCodes))
    when(codeListsRepository.buildExciseProducts(equalTo(clientSession), any()))
      .thenReturn(Future.successful(ExciseProducts))
    when(exciseProductsRepository.replaceExciseProducts(equalTo(clientSession), any()))
      .thenReturn(Future.unit)
    when(cnCodesRepository.replaceCnCodes(equalTo(clientSession), any()))
      .thenReturn(Future.failed(MongoError.NotAcknowledged))

    refDataJob.importReferenceData().failed.futureValue shouldBe a[MongoError]

    verify(codeListsRepository, atLeastOnce()).saveCodeListEntries(
      equalTo(clientSession),
      any()
    )
    verify(cnCodesRepository, times(1)).replaceCnCodes(equalTo(clientSession), any())

    verify(clientSession, times(1)).abortTransaction()
  }
}
