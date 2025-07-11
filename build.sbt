import uk.gov.hmrc.DefaultBuildSettings

val appName = "emcs-tfe-crdl-reference-data"

ThisBuild / scalaVersion := "3.3.6"
ThisBuild / majorVersion := 1

lazy val microservice = (project in file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(
    name := appName,
    libraryDependencies ++= AppDependencies(),
    PlayKeys.playDefaultPort := 8312,
    // Change classloader layering to avert classloading issues
    Compile / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat,
    // https://www.scala-lang.org/2021/01/12/configuring-and-suppressing-warnings.html
    // suppress warnings in generated routes files
    scalacOptions ++= Seq(
      "-Wconf:src=routes/.*:s",
      "-Wconf:msg=Flag.*repeatedly:s"
    ),
  )
  .settings(CodeCoverageSettings.settings *)

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(DefaultBuildSettings.itSettings())
  .settings(
    libraryDependencies ++= AppDependencies.it,
    // Change classloader layering to avert classloading issues
    Compile / classLoaderLayeringStrategy := ClassLoaderLayeringStrategy.Flat,
    // Disable duplicate compiler option warning as it's caused by our sbt plugins
    scalacOptions += "-Wconf:msg=Flag.*repeatedly:s",
    // Uncomment to disable Oracle tests
    Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-l", "uk.gov.hmrc.emcstfereferencedata.support.OracleDb")
  )

