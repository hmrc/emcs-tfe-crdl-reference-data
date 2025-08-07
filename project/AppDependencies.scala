import play.sbt.PlayImport.*
import sbt.*

object AppDependencies {

  val playSuffix = "-play-30"

  val hmrcBootstrapVersion = "9.18.0"
  val hmrcMongoVersion     = "2.7.0"
  val quartzVersion        = "2.5.0"

  private val compile = Seq(
    "uk.gov.hmrc"         %% s"bootstrap-backend$playSuffix" % hmrcBootstrapVersion,
    "uk.gov.hmrc.mongo"   %% s"hmrc-mongo$playSuffix"        % hmrcMongoVersion,
    "org.quartz-scheduler" % "quartz"                        % quartzVersion,
    "com.lihaoyi"         %% "sourcecode"                    % "0.4.2"
  )

  private val test = Seq(
    "uk.gov.hmrc"       %% s"bootstrap-test$playSuffix"  % hmrcBootstrapVersion,
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-test$playSuffix" % hmrcMongoVersion
  ).map(_ % Test)

  val it: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% s"bootstrap-test$playSuffix"  % hmrcBootstrapVersion,
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-test$playSuffix" % hmrcMongoVersion
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}
