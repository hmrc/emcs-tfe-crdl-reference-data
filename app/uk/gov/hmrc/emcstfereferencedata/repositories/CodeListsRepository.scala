package uk.gov.hmrc.emcstfereferencedata.repositories

import org.mongodb.scala.*
import org.mongodb.scala.model.Filters.*
import org.mongodb.scala.model.{IndexModel, IndexOptions, Indexes}
import play.api.libs.json.{Format, JsBoolean, JsValue, Reads, Writes}
import uk.gov.hmrc.emcstfereferencedata.models.crdl.CrdlCodeListEntry
import uk.gov.hmrc.emcstfereferencedata.models.mongo.{CodeListCode, CodeListEntry}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}
import uk.gov.hmrc.mongo.transaction.Transactions

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class CodeListsRepository @Inject() (val mongoComponent: MongoComponent)(using ec: ExecutionContext)
  extends PlayMongoRepository[CodeListEntry](
    mongoComponent,
    collectionName = "codelists",
    domainFormat = CodeListEntry.format,
    extraCodecs =
      Codecs.playFormatSumCodecs[JsValue](Format(Reads.JsValueReads, Writes.jsValueWrites)) ++
        Codecs.playFormatSumCodecs[JsBoolean](Format(Reads.JsBooleanReads, Writes.jsValueWrites)),
    indexes = Seq(
      IndexModel(
        Indexes.ascending("codeListCode", "key"),
        IndexOptions().unique(true)
      )
    )
  ) with Transactions {

  // This collection's entries are cleared every time new codelists are imported
  override lazy val requiresTtlIndex: Boolean = false

  def saveCodeListEntries(
    session: ClientSession,
    codeListCode: CodeListCode,
    crdlEntries: List[CrdlCodeListEntry]
  ): Future[Unit] =
    for {
      _ <- collection.deleteMany(session, equal("codeListCode", codeListCode.value)).toFuture()
      entries = crdlEntries.map(CodeListEntry.fromCrdlEntry(codeListCode, _))
      _ <- collection.insertMany(session, entries).toFuture()
    } yield ()
}
