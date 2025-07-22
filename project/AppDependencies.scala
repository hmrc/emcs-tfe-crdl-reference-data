import play.sbt.PlayImport.*
import sbt.*

object AppDependencies {

  val playSuffix = "-play-30"

  val hmrcBootstrapVersion = "9.14.0"
  val hmrcMongoVersion     = "2.6.0"
  val scalamockVersion     = "7.4.0"
  val catsCoreVersion      = "2.13.0"
  val quartzVersion        = "2.5.0"

  private val compile = Seq(
    "uk.gov.hmrc"         %% s"bootstrap-backend$playSuffix" % hmrcBootstrapVersion,
    "uk.gov.hmrc.mongo"   %% s"hmrc-mongo$playSuffix"        % hmrcMongoVersion,
    "org.typelevel"       %% "cats-core"                     % catsCoreVersion,
    "org.quartz-scheduler" % "quartz"                        % quartzVersion
  )

  private val test = Seq(
    "uk.gov.hmrc"       %% s"bootstrap-test$playSuffix"  % hmrcBootstrapVersion,
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-test$playSuffix" % hmrcMongoVersion,
    "org.scalamock"     %% "scalamock"                   % scalamockVersion
  ).map(_ % Test)

  val it: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% s"bootstrap-test$playSuffix"  % hmrcBootstrapVersion,
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-test$playSuffix" % hmrcMongoVersion
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
