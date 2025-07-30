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

import play.api.libs.json.{Json, Reads}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.emcstfereferencedata.connector.RetrievePackagingTypesConnector
import uk.gov.hmrc.emcstfereferencedata.controllers.predicates.{AuthAction, AuthActionHelper}
import uk.gov.hmrc.emcstfereferencedata.models.response.ErrorResponse.NoDataReturnedFromDatabaseError
import uk.gov.hmrc.emcstfereferencedata.utils.Logging
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class RetrievePackagingTypesController @Inject() (
  cc: ControllerComponents,
  connector: RetrievePackagingTypesConnector,
  override val auth: AuthAction
)(implicit ec: ExecutionContext)
  extends BackendController(cc)
  with AuthActionHelper
  with Logging {

  def showAllPackagingTypes(isCountable: Option[Boolean]): Action[AnyContent] =
    authorisedUserGetRequest { implicit request =>
      connector
        .retrievePackagingTypes(packagingTypeCodes = None, isCountable)
        .map { response =>
          if (response.nonEmpty) {
            Ok(Json.toJson(response))
          } else {
            logger.warn("No data returned for all packaging types")
            InternalServerError(Json.toJson(NoDataReturnedFromDatabaseError))
          }
        }
    }

  def show: Action[Set[String]] =
    authorisedUserPostRequest(Reads.of[Set[String]]) { implicit request =>
      connector
        .retrievePackagingTypes(packagingTypeCodes = Some(request.body), isCountable = None)
        .map { response =>
          if (response.nonEmpty) {
            Ok(Json.toJson(response))
          } else {
            logger.warn(
              s"No data returned for input packaging types: ${request.body.mkString(",")}"
            )
            InternalServerError(Json.toJson(NoDataReturnedFromDatabaseError))
          }
        }
    }
}
