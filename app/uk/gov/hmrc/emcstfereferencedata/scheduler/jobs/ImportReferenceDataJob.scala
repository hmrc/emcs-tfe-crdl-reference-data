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
import uk.gov.hmrc.emcstfereferencedata.connector.CrdlConnector
import uk.gov.hmrc.emcstfereferencedata.models.crdl.{CodeListCode, CodeSet}
import uk.gov.hmrc.emcstfereferencedata.repositories.{CnCodesRepository, CodeListsRepository, ExciseProductsRepository}
import uk.gov.hmrc.emcstfereferencedata.utils.Logging
import uk.gov.hmrc.http.HeaderCarrier
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

  given HeaderCarrier = HeaderCarrier()

  given TransactionConfiguration = TransactionConfiguration.strict

  override val lockId: String = jobName
  override val ttl: Duration  = 1.hour

  private def refreshCodeListEntries(session: ClientSession, codeListCodes: Seq[CodeListCode]) = {
    def futureMap[A, B](keys: Seq[A])(func: A => Future[Seq[B]]): Future[Map[A, Seq[B]]] =
      Future.traverse(keys)(key => func(key).map(values => (key, values))).map(_.toMap)

    for {
      codeToEntries <- futureMap(codeListCodes) {
        crdlConnector.fetchCodeList(_, filterKeys = None, filterProperties = None)
      }
      _ <- codeListsRepository.saveCodeListEntries(session, codeToEntries)
    } yield ()
  }

  private def rebuildExciseProducts(session: ClientSession, codeSets: Seq[CodeSet]): Future[Unit] = {
    for {
      // We need both BC36 and BC66 data (and their HMC prefixed counterparts) to build the excise-products collection
      _ <- refreshCodeListEntries(session, codeSets.map(_.productCategories))

      exciseProducts <- Future.sequence(codeSets.map(codeListsRepository.buildExciseProducts(session, _)))
      _              <- exciseProductsRepository.replaceExciseProducts(session, exciseProducts.flatten)
    } yield ()
  }

  private def rebuildCnCodes(session: ClientSession, codeSets: Seq[CodeSet]): Future[Unit] = {
    for {
      // We need E200, BC36 and BC37 data (and their HMRC prefixed counterparts) to build the cn-codes collection
      _ <- refreshCodeListEntries(session, codeSets.map(_.cnCodes))
      _ <- refreshCodeListEntries(session, codeSets.map(_.cnCodeExciseProductCorrespondence))

      cnCodeInfo <- Future.sequence(codeSets.map(codeListsRepository.buildCnCodes(session, _)))
      _          <- cnCodesRepository.replaceCnCodes(session, cnCodeInfo.flatten)
    } yield ()
  }

  private[jobs] def importReferenceData(): Future[Unit] = {
    val importRefData = withSessionAndTransaction { session =>
      // excise products data (BC36/HMRCBC36) is used by both of the derived collections
      for {
        _ <- refreshCodeListEntries(session, CodeSet.values.map(_.exciseProducts))
        _ <- rebuildCnCodes(session, CodeSet.values)
        _ <- rebuildExciseProducts(session, CodeSet.values)
      } yield ()
    }

    importRefData.foreach(_ => logger.info(s"$jobName job completed successfully"))
    importRefData.failed.foreach(err => logger.error(s"$jobName job failed", err))

    importRefData
  }

  override def execute(context: JobExecutionContext): Unit =
    Await.result(
      withLock(importReferenceData()).map {
        _.getOrElse {
          logger.info(s"$jobName job lock could not be obtained")
        }
      },
      Duration.Inf
    )
}
