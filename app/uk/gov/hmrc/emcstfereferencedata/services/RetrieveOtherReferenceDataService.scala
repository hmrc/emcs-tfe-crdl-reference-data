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

package uk.gov.hmrc.emcstfereferencedata.services

import uk.gov.hmrc.emcstfereferencedata.connector.CrdlConnector
import uk.gov.hmrc.emcstfereferencedata.models.crdl.CodeListCode
import uk.gov.hmrc.emcstfereferencedata.models.crdl.CodeListCode.*
import uk.gov.hmrc.http.HeaderCarrier

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class RetrieveOtherReferenceDataService @Inject()(connector: CrdlConnector) {

  def retrieveWineOperations(filterKeys: Option[Set[String]])(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Map[String, String]] =
    retrieveOtherReferenceData(BC41, filterKeys)

  def retrieveMemberStates()(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Map[String, String]] =
    retrieveOtherReferenceData(BC11, filterKeys = None)

  def retrieveCountries()(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Map[String, String]] =
    retrieveOtherReferenceData(BC08, filterKeys = None)

  def retrieveTransportUnits()(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Map[String, String]] =
    retrieveOtherReferenceData(BC35, filterKeys = None)

  def retrieveTypesOfDocument()(implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Map[String, String]] =
    retrieveOtherReferenceData(BC106, filterKeys = None)

  def retrieveOtherReferenceData(codeListCode: CodeListCode, filterKeys: Option[Set[String]])(
    implicit
    hc: HeaderCarrier,
    ec: ExecutionContext
  ): Future[Map[String, String]] =
    connector
      .fetchCodeList(codeListCode, filterKeys, filterProperties = None)
      .map { codeListEntries =>
          codeListEntries.map(entry => entry.key -> entry.value).toMap
      }

}
