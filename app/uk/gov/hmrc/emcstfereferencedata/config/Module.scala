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

package uk.gov.hmrc.emcstfereferencedata.config

import com.google.inject.{AbstractModule, Singleton}
import uk.gov.hmrc.emcstfereferencedata.connector.retrieveAllCNCodes.{RetrieveAllCNCodesConnector, RetrieveAllCNCodesConnectorCRDL}
import uk.gov.hmrc.emcstfereferencedata.connector.retrieveAllEPCCodes.{RetrieveAllEPCCodesConnector, RetrieveAllEPCCodesConnectorCRDL}
import uk.gov.hmrc.emcstfereferencedata.connector.retrieveCnCodeInformation.*
import uk.gov.hmrc.emcstfereferencedata.connector.retrieveOtherReferenceData.{RetrieveOtherReferenceDataConnector, RetrieveOtherReferenceDataConnectorCRDL}
import uk.gov.hmrc.emcstfereferencedata.connector.retrievePackagingTypes.{RetrievePackagingTypesConnector, RetrievePackagingTypesConnectorCRDL}
import uk.gov.hmrc.emcstfereferencedata.connector.retrieveProductCodes.{RetrieveProductCodesConnector, RetrieveProductCodesConnectorCRDL}
import uk.gov.hmrc.emcstfereferencedata.controllers.predicates.{AuthAction, AuthActionImpl}

@Singleton
class Module extends AbstractModule {
  override def configure(): Unit = {
    bind(classOf[AppConfig]).asEagerSingleton()
    bind(classOf[AuthAction]).to(classOf[AuthActionImpl])
    bind(classOf[RetrieveAllCNCodesConnector]).to(classOf[RetrieveAllCNCodesConnectorCRDL])
    bind(classOf[RetrieveAllEPCCodesConnector]).to(classOf[RetrieveAllEPCCodesConnectorCRDL])
    bind(classOf[RetrieveCnCodeInformationConnector]).to(classOf[RetrieveCnCodeInformationConnectorCRDL])
    bind(classOf[RetrieveOtherReferenceDataConnector]).to(classOf[RetrieveOtherReferenceDataConnectorCRDL])
    bind(classOf[RetrievePackagingTypesConnector]).to(classOf[RetrievePackagingTypesConnectorCRDL])
    bind(classOf[RetrieveProductCodesConnector]).to(classOf[RetrieveProductCodesConnectorCRDL])
  }
}
