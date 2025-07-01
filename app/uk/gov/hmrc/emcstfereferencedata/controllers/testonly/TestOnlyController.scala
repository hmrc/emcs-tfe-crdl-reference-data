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

import org.mongodb.scala.*
import org.mongodb.scala.model.Filters
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.emcstfereferencedata.repositories.{CnCodesRepository, ExciseProductsRepository, CodeListsRepository}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext


  @Singleton
  class TestOnlyController @Inject() (
    cc: ControllerComponents,
    codeListsRepository: CodeListsRepository,
    exciseProductsRepository: ExciseProductsRepository,
    cnCodesRepository: CnCodesRepository
  )(using ec: ExecutionContext)
    extends BackendController(cc) {

    def deleteCodeLists(): Action[AnyContent] = Action.async {
      codeListsRepository.collection.deleteMany(Filters.empty()).toFuture().map {
        case result if result.wasAcknowledged() => Ok
        case _                                  => InternalServerError
      }
    }
    
    def deleteExciseProducts(): Action[AnyContent] = Action.async {
      exciseProductsRepository.collection.deleteMany(Filters.empty()).toFuture().map {
        case result if result.wasAcknowledged() => Ok
        case _                                  => InternalServerError
      }
    }
    
    def deleteCnCodes(): Action[AnyContent] = Action.async {
      cnCodesRepository.collection.deleteMany(Filters.empty()).toFuture().map {
        case result if result.wasAcknowledged() => Ok
        case _                                  => InternalServerError
      }
    }
  }

