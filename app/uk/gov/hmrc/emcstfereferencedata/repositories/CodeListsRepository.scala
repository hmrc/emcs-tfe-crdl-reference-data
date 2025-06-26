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

import com.mongodb.client.model.Variable
import org.mongodb.scala.*
import org.mongodb.scala.bson.{BsonArray, BsonDocument}
import org.mongodb.scala.model.Aggregates.*
import org.mongodb.scala.model.Filters.*
import org.mongodb.scala.model.Projections.*
import org.mongodb.scala.model.{IndexModel, IndexOptions, Indexes}
import play.api.libs.json.*
import uk.gov.hmrc.emcstfereferencedata.models.crdl.CrdlCodeListEntry
import uk.gov.hmrc.emcstfereferencedata.models.errors.MongoError
import uk.gov.hmrc.emcstfereferencedata.models.mongo.{CodeListCode, CodeListEntry}
import uk.gov.hmrc.emcstfereferencedata.models.response.ExciseProductCode
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}
import uk.gov.hmrc.mongo.transaction.Transactions

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CodeListsRepository @Inject() (val mongoComponent: MongoComponent)(using ec: ExecutionContext)
  extends PlayMongoRepository[CodeListEntry](
    mongoComponent,
    collectionName = "codeLists",
    domainFormat = CodeListEntry.format,
    extraCodecs =
      Codecs.playFormatSumCodecs[JsValue](Format(Reads.JsValueReads, Writes.jsValueWrites)) ++
        Codecs.playFormatSumCodecs[JsBoolean](Format(Reads.JsBooleanReads, Writes.jsValueWrites)) :+
        Codecs.playFormatCodec(ExciseProductCode.mongoFormat),
    indexes = Seq(
      IndexModel(
        Indexes.ascending("codeListCode", "key"),
        IndexOptions().unique(true)
      ),
      IndexModel(Indexes.ascending("codeListCode"))
    )
  )
  with Transactions {

  // This collection's entries are cleared every time new codelists are imported
  override lazy val requiresTtlIndex: Boolean = false

  private val ExciseProducts    = "BC36"
  private val ProductCategories = "BC66"

  def buildExciseProducts(session: ClientSession): Future[Seq[ExciseProductCode]] = {
    collection
      .aggregate[ExciseProductCode](
        session,
        List(
          // Find the excise products
          filter(equal("codeListCode", ExciseProducts)),
          lookup(
            from = "codeLists",
            // Alias the excise product category to "category"
            let = Seq(Variable("category", "$properties.exciseProductsCategoryCode")),
            pipeline = Seq(
              filter(
                expr(
                  BsonDocument(
                    "$and" -> BsonArray(
                      // Find the product categories
                      BsonDocument("$eq" -> BsonArray("$codeListCode", ProductCategories)),
                      // Find the one where the category is equal to the outer document's category
                      BsonDocument("$eq" -> BsonArray("$key", "$$category"))
                    )
                  )
                )
              )
            ),
            // Project the result as productCategory
            as = "productCategory"
          ),
          project(
            fields(
              // Include the excise product key as "code"
              computed("code", "$key"),
              // Include the excise product value as "description"
              computed("description", "$value"),
              computed(
                // Get the "category" from the nested "productCategory" doc's key
                "category",
                BsonDocument(
                  "$getField" -> BsonDocument(
                    "field" -> "key",
                    "input" -> BsonDocument("$first" -> "$productCategory")
                  )
                )
              ),
              computed(
                // Get the "categoryDescription" from the nested "productCategory" doc's value
                "categoryDescription",
                BsonDocument(
                  "$getField" -> BsonDocument(
                    "field" -> "value",
                    "input" -> BsonDocument("$first" -> "$productCategory")
                  )
                )
              )
            )
          )
        )
      )
      .toFuture()
  }

  // TODO: Make public when implementing test-only endpoints
  private def deleteCodeListEntries(
    session: ClientSession,
    codeListCode: Option[CodeListCode]
  ): Future[Unit] =
    collection
      .deleteMany(
        session,
        codeListCode
          .map(code => equal("codeListCode", code.value))
          .getOrElse(empty())
      )
      .toFuture()
      .map { result =>
        if (!result.wasAcknowledged())
          throw MongoError.NotAcknowledged
      }

  def saveCodeListEntries(
    session: ClientSession,
    codeListCode: CodeListCode,
    crdlEntries: List[CrdlCodeListEntry]
  ): Future[Unit] =
    for {
      _ <- deleteCodeListEntries(session, Some(codeListCode))

      entries = crdlEntries.map(CodeListEntry.fromCrdlEntry(codeListCode, _))

      _ <- collection.insertMany(session, entries).toFuture().map { result =>
        if (!result.wasAcknowledged())
          throw MongoError.NotAcknowledged
      }
    } yield ()
}
