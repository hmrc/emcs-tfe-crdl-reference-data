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

package uk.gov.hmrc.emcstfereferencedata.controllers.testonly

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.emcstfereferencedata.models.errors.MongoError
import uk.gov.hmrc.emcstfereferencedata.repositories.{
  CnCodesRepository,
  CodeListsRepository,
  ExciseProductsRepository
}
import uk.gov.hmrc.emcstfereferencedata.scheduler.JobScheduler
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.transaction.{TransactionConfiguration, Transactions}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class TestOnlyController @Inject() (
  cc: ControllerComponents,
  codeListsRepository: CodeListsRepository,
  exciseProductsRepository: ExciseProductsRepository,
  cnCodesRepository: CnCodesRepository,
  jobScheduler: JobScheduler,
  val mongoComponent: MongoComponent
)(using ec: ExecutionContext)
  extends BackendController(cc)
  with Transactions {

  given tc: TransactionConfiguration = TransactionConfiguration.strict

  def importReferenceData(): Action[AnyContent] = Action {
    jobScheduler.startReferenceDataImport()
    Accepted
  }

  def referenceDataImportStatus(): Action[AnyContent] = Action {
    Ok(Json.toJson(jobScheduler.referenceDataImportStatus()))
  }

  def deleteCodeLists(): Action[AnyContent] = Action.async {
    withClientSession { session =>
      codeListsRepository
        .deleteCodeListEntries(session, None)
        .map { _ =>
          Ok
        }
        .recover { case error: MongoError => InternalServerError }
    }
  }

  def deleteExciseProducts(): Action[AnyContent] = Action.async {
    withClientSession { session =>
      exciseProductsRepository
        .deleteExciseProducts(session)
        .map { _ =>
          Ok
        }
        .recover { case error: MongoError => InternalServerError }
    }
  }

  def deleteCnCodes(): Action[AnyContent] = Action.async {
    withClientSession { session =>
      cnCodesRepository
        .deleteCnCodes(session)
        .map { _ =>
          Ok
        }
        .recover { case error: MongoError => InternalServerError }
    }
  }
}
