import play.sbt.PlayImport.*
import sbt.*

object AppDependencies {

  val playSuffix        = "-play-30"

  val hmrcBootstrapVersion = "9.13.0"
  val scalamockVersion     = "7.3.2"
  val catsCoreVersion      = "2.13.0"
  val oraVersion           = "19.3.0.0"
  val jsoupVersion         = "1.20.1"

  private val compile = Seq(
    "uk.gov.hmrc"               %% s"bootstrap-backend$playSuffix"    % hmrcBootstrapVersion,
    "com.oracle.jdbc"           %   "ojdbc8"                          % oraVersion,
    "com.oracle.jdbc"           %   "orai18n"                         % oraVersion,
    "org.typelevel"             %%  "cats-core"                       % catsCoreVersion,
    "org.jsoup"                 %   "jsoup"                           % jsoupVersion,
    jdbc
  )

  private val test = Seq(
    "uk.gov.hmrc"               %% s"bootstrap-test$playSuffix"       % hmrcBootstrapVersion,
    "org.scalamock"             %% "scalamock"                        % scalamockVersion,
    "org.jsoup"                 % "jsoup"                             % jsoupVersion,
  ).map(_ % Test)

  val it: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% s"bootstrap-test$playSuffix" % hmrcBootstrapVersion % Test
  )

  def apply(): Seq[ModuleID] = compile ++ test

}
