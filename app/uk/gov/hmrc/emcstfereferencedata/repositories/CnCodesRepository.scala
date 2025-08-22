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

package uk.gov.hmrc.emcstfereferencedata.repositories

import org.mongodb.scala.*
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.*
import org.mongodb.scala.model.Sorts.*
import org.mongodb.scala.model.{Filters, IndexModel, IndexOptions, Indexes}
import uk.gov.hmrc.emcstfereferencedata.models.errors.MongoError
import uk.gov.hmrc.emcstfereferencedata.models.request.CnInformationRequest
import uk.gov.hmrc.emcstfereferencedata.models.response.CnCodeInformation
import uk.gov.hmrc.emcstfereferencedata.utils.Logging
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.transaction.Transactions

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CnCodesRepository @Inject() (val mongoComponent: MongoComponent)(using
  ec: ExecutionContext
) extends PlayMongoRepository[CnCodeInformation](
    mongoComponent,
    collectionName = "cnCodes",
    domainFormat = CnCodeInformation.mongoFormat,
    indexes = Seq(
      IndexModel(
        // A CN code can belong to multiple excise products,
        // and an excise product can be associated with multiple CN codes.
        Indexes.ascending("exciseProductCode", "cnCode"),
        IndexOptions().unique(true)
      )
    )
  )
  with Transactions
  with Logging {

  // This collection's entries are cleared every time new codelists are imported
  override lazy val requiresTtlIndex: Boolean = false

  def deleteCnCodes(session: ClientSession): Future[Unit] =
    collection
      .deleteMany(session, empty())
      .toFuture()
      .map { result =>
        if (!result.wasAcknowledged())
          throw MongoError.NotAcknowledged
      }

  def saveCnCodes(
    session: ClientSession,
    cnCodes: Seq[CnCodeInformation]
  ): Future[Unit] =
    if (cnCodes.isEmpty) {
      logger.error(
        "CnCodes List received from CRDL-Cache was empty"
      )
      Future.failed(MongoError.NoDataToInsert)
    } else

      for {
        _ <- deleteCnCodes(session)

        _ <- collection.insertMany(session, cnCodes).toFuture().map { result =>
          if (!result.wasAcknowledged())
            throw MongoError.NotAcknowledged
        }
      } yield ()

  def fetchCnCodesForProduct(exciseProductCode: String): Future[Seq[CnCodeInformation]] =
    collection
      .find(equal("exciseProductCode", exciseProductCode))
      .sort(ascending("cnCode"))
      .toFuture()

  def fetchCnCodeInformation(
    cnInformationRequest: CnInformationRequest
  ): Future[Map[String, CnCodeInformation]] = {
    val filters: Seq[Bson] = cnInformationRequest.items.map { item =>
      and(
        equal("exciseProductCode", item.productCode),
        equal("cnCode", item.cnCode)
      )
    }
    collection
      .find(or(filters*))
      .toFuture()
      .map(seq => seq.map(item => item.cnCode -> item).toMap)
  }

}
