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

import org.mongodb.scala.ClientSession
import org.quartz.{DisallowConcurrentExecution, Job, JobExecutionContext}
import uk.gov.hmrc.emcstfereferencedata.connector.crdl.CrdlConnector
import uk.gov.hmrc.emcstfereferencedata.models.crdl.CodeListCode
import uk.gov.hmrc.emcstfereferencedata.models.crdl.CodeListCode.{BC36, BC37, BC66, E200}
import uk.gov.hmrc.emcstfereferencedata.repositories.{
  CnCodesRepository,
  CodeListsRepository,
  ExciseProductsRepository
}
import uk.gov.hmrc.emcstfereferencedata.utils.Logging
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.lock.{LockService, MongoLockRepository}
import uk.gov.hmrc.mongo.transaction.{TransactionConfiguration, Transactions}

import javax.inject.Inject
import scala.concurrent.duration.*
import scala.concurrent.{Await, ExecutionContext, Future}

@DisallowConcurrentExecution
class ImportReferenceDataJob @Inject() (
  val mongoComponent: MongoComponent,
  val lockRepository: MongoLockRepository,
  crdlConnector: CrdlConnector,
  codeListsRepository: CodeListsRepository,
  cnCodesRepository: CnCodesRepository,
  exciseProductsRepository: ExciseProductsRepository
)(using ec: ExecutionContext)
  extends Job
  with LockService
  with Logging
  with Transactions {

  private val jobName = "import-reference-data"

  given TransactionConfiguration = TransactionConfiguration.strict

  override val lockId: String = jobName
  override val ttl: Duration  = 1.hour

  private def refreshCodeListEntries(session: ClientSession, codeListCode: CodeListCode) =
    for {
      entries <- crdlConnector.fetchCodeList(codeListCode)
      _       <- codeListsRepository.saveCodeListEntries(session, codeListCode, entries)
    } yield ()

  private def rebuildExciseProducts(
    session: ClientSession,
    saveExciseProducts: Future[Unit]
  ): Future[Unit] = {
    val saveProductCategories = refreshCodeListEntries(session, BC66)

    for {
      // We need both BC36 and BC66 data to build the excise-products collection
      _ <- saveExciseProducts
      _ <- saveProductCategories

      exciseProducts <- codeListsRepository.buildExciseProducts(session)
      _              <- exciseProductsRepository.saveExciseProducts(session, exciseProducts)

    } yield ()
  }

  private def rebuildCnCodes(
    session: ClientSession,
    saveExciseProducts: Future[Unit]
  ): Future[Unit] = {
    val saveCnCodes         = refreshCodeListEntries(session, BC37)
    val saveCorrespondences = refreshCodeListEntries(session, E200)

    for {
      // We need E200, BC36 and BC37 data to build the cn-codes collection
      _ <- saveCorrespondences
      _ <- saveExciseProducts
      _ <- saveCnCodes

      cnCodeInfo <- codeListsRepository.buildCnCodes(session)
      _          <- cnCodesRepository.saveCnCodes(session, cnCodeInfo)

    } yield ()
  }

  private[jobs] def importReferenceData(): Future[Unit] = {
    val importRefData = withSessionAndTransaction { session =>
      // BC36 data is used by both of the derived collections
      val saveExciseProducts = refreshCodeListEntries(session, BC36)
      val cnCodes            = rebuildCnCodes(session, saveExciseProducts)
      val exciseProducts     = rebuildExciseProducts(session, saveExciseProducts)
      cnCodes.zip(exciseProducts).map(_ => ())
    }

    importRefData.foreach(_ => logger.info(s"${jobName} job completed successfully"))
    importRefData.failed.foreach(err => logger.error(s"${jobName} job failed", err))

    importRefData
  }

  override def execute(context: JobExecutionContext): Unit =
    Await.result(
      withLock(importReferenceData()).map {
        _.getOrElse {
          logger.info(s"${jobName} job lock could not be obtained")
        }
      },
      Duration.Inf
    )
}
