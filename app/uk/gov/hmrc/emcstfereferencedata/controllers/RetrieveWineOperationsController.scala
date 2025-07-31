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
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext
import play.api.libs.json.Reads
import uk.gov.hmrc.emcstfereferencedata.models.response.ErrorResponse.NoDataReturnedFromDatabaseError
import uk.gov.hmrc.emcstfereferencedata.services.RetrieveOtherReferenceDataService
import uk.gov.hmrc.emcstfereferencedata.utils.Logging

@Singleton
class RetrieveWineOperationsController @Inject()(
                                                  cc: ControllerComponents,
                                                  connector: RetrieveOtherReferenceDataService,
                                                  override val auth: AuthAction
                                                )(implicit ec: ExecutionContext)
  extends BackendController(cc)
    with AuthActionHelper with Logging {

  def showAllWineOperations: Action[AnyContent] = authorisedUserGetRequest { implicit request =>
    connector.retrieveWineOperations(filterKeys = None).map { response =>
      if (response.nonEmpty) {
        Ok(Json.toJson(response))
      }
      else {
        logger.warn("No data returned for all wine operations")
        InternalServerError(Json.toJson(NoDataReturnedFromDatabaseError))
      }
    }
  }

  def show: Action[Set[String]] = authorisedUserPostRequest(Reads.of[Set[String]]) {
    implicit request =>
      connector.retrieveWineOperations(filterKeys = Some(request.body)).map { response =>
        if (response.nonEmpty) {
          Ok(Json.toJson(response))
        }
        else {
          logger.warn(
            s"No data returned for input wine operations: ${request.body.mkString(",")}"
          )
          InternalServerError(Json.toJson(NoDataReturnedFromDatabaseError))
        }
      }
  }

}
