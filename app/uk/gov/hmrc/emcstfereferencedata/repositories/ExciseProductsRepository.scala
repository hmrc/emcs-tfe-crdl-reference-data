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
import org.mongodb.scala.model.Filters.*
import org.mongodb.scala.model.Sorts.ascending
import org.mongodb.scala.model.{IndexModel, IndexOptions, Indexes}
import uk.gov.hmrc.emcstfereferencedata.models.errors.MongoError
import uk.gov.hmrc.emcstfereferencedata.models.request.CnInformationRequest
import uk.gov.hmrc.emcstfereferencedata.models.response.{CnCodeInformation, ExciseProductCode}
import uk.gov.hmrc.emcstfereferencedata.utils.Logging
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.transaction.Transactions

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ExciseProductsRepository @Inject() (val mongoComponent: MongoComponent)(using
  ec: ExecutionContext
) extends PlayMongoRepository[ExciseProductCode](
    mongoComponent,
    collectionName = "exciseProducts",
    domainFormat = ExciseProductCode.mongoFormat,
    indexes = Seq(
      IndexModel(
        Indexes.ascending("code"),
        IndexOptions().unique(true)
      ),
      IndexModel(Indexes.ascending("category"))
    )
  )
  with Transactions with Logging {

  // This collection's entries are cleared every time new codelists are imported
  override lazy val requiresTtlIndex: Boolean = false

  def deleteExciseProducts(session: ClientSession): Future[Unit] =
    collection
      .deleteMany(session, empty())
      .toFuture()
      .map { result =>
        if (!result.wasAcknowledged())
          throw MongoError.NotAcknowledged
      }

  def saveExciseProducts(
    session: ClientSession,
    exciseProducts: Seq[ExciseProductCode]
  ): Future[Unit] =
    if (exciseProducts.isEmpty) {
      logger.error(
        "ExciseProducts list received from CRDL-Cache was empty"
      )
      Future.failed(MongoError.NoDataToInsert)
    } else

      for {
        _ <- deleteExciseProducts(session)
        _ <- collection.insertMany(session, exciseProducts).toFuture().map { result =>
          if (!result.wasAcknowledged())
            throw MongoError.NotAcknowledged
        }
      } yield ()

  def fetchExciseProductsForCategory(categoryCode: String): Future[Seq[ExciseProductCode]] =
    collection
      .find(equal("category", categoryCode))
      .sort(ascending("code"))
      .toFuture()

  def fetchAllEPCCodes(): Future[Seq[ExciseProductCode]] =
    collection
      .find()
      .sort(ascending("code"))
      .toFuture()

  def fetchProductCodesInformation(
    cnInformationRequest: CnInformationRequest
  ): Future[Map[String, CnCodeInformation]] =
    collection
      .find(in("code", cnInformationRequest.items.map(_.productCode)*))
      .sort(ascending("code"))
      .toFuture()
      .map { products =>
        val productsByCode = products.map(product => product.code -> product).toMap
        cnInformationRequest.items.flatMap { item =>
          productsByCode.get(item.productCode).map { product =>
            val cnInfo = CnCodeInformation(
              cnCode = item.cnCode,
              cnCodeDescription = product.description,
              exciseProductCode = product.code,
              exciseProductCodeDescription = product.description,
              unitOfMeasureCode = product.unitOfMeasureCode
            )
            item.cnCode -> cnInfo
          }
        }.toMap
      }
}
