/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.emcstfereferencedata.controllers

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.emcstfereferencedata.controllers.predicates.{AuthAction, AuthActionHelper}
import uk.gov.hmrc.emcstfereferencedata.services.RetrieveWineOperationsService
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class RetrieveWineOperationsController @Inject()(cc: ControllerComponents,
                                                 service: RetrieveWineOperationsService,
                                                 override val auth: AuthAction
                                                )(implicit ec: ExecutionContext) extends BackendController(cc) with AuthActionHelper {


  def showAllWineOperations: Action[AnyContent] = authorisedUserGetRequest {
    implicit request =>
      service.retrieveWineOperations().map {
        case Right(response) =>
          Ok(Json.toJson(response))
        case Left(error) =>
          InternalServerError(Json.toJson(error))
      }
  }

  def show: Action[Seq[String]] = authorisedUserPostRequest {
    json =>
      for {
        wineOperationsList <- json.validate[Seq[String]]
      } yield wineOperationsList
  } {
    implicit request =>
      service.retrieveWineOperations(request.body).map {
        case Right(response) =>
          Ok(Json.toJson(response))
        case Left(error) =>
          InternalServerError(Json.toJson(error))
      }
  }

}
