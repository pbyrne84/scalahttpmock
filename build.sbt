organization := "com.github.pbyrne84"

name := "scalahttpmock"

version := "0.1-SNAPSHOT"

lazy val scala213 = "2.13.8"
lazy val scala3 = "3.2.2"

scalaVersion := scala3

val circeVersion = "0.14.2"

val zioVersion: String = "2.0.7"
val sttpVersion: String = "3.8.11"

// even though free port is detected it can still race across tests
Test / parallelExecution := false

lazy val supportedScalaVersions = List(scala3, scala213)

lazy val commonSettings = Seq(
  scalaVersion := scala3,
  crossScalaVersions := supportedScalaVersions,
  scalacOptions ++= Seq(
    "-encoding",
    "utf8",
    "-feature",
    "-language:implicitConversions",
    "-language:existentials",
    "-unchecked"
    // "-no-indent" - only for scala3 not cross build
  ),
  Test / parallelExecution := false,
  // crossScalaVersions := supportedScalaVersions,
  libraryDependencies ++= Seq(
    // Optional for auto-derivation of JSON codecs
    "io.circe" %% "circe-generic" % circeVersion,
    // Optional for string interpolation to JSON model
    "io.circe" %% "circe-literal" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion,
    "org.typelevel" %% "cats-effect" % "3.3.14",
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
    "io.netty" % "netty-all" % "4.1.89.Final",
    "com.softwaremill.sttp.client3" %% "core" % sttpVersion % Test,
    "com.softwaremill.sttp.client3" %% "cats" % sttpVersion % Test,
    "org.mockito" % "mockito-core" % "4.6.1" % Test,
    "org.typelevel" %% "cats-effect-testing-scalatest" % "1.5.0" % Test,
    "org.scalatest" %% "scalatest" % "3.2.13" % Test
  ),
  formatAndTest := {
    (Test / test)
      .dependsOn(Compile / scalafmtAll)
      .dependsOn(Test / scalafmtAll)
  }.value,
  Test / test := (Test / test)
    .dependsOn(Compile / scalafmtCheck)
    .dependsOn(Test / scalafmtCheck)
    .value
)

//not to be used in ci, intellij has got a bit bumpy in the format on save on optimize imports across the project
val formatAndTest =
  taskKey[Unit]("format all code then run tests, do not use on CI as any changes will not be committed")

lazy val root = (project in file("."))
  .settings(commonSettings)
  .aggregate(scalaHttpMockCore, scalaHttpMockZio)

lazy val scalaHttpMockCore = (project in file("modules/core"))
  .settings(
    commonSettings
  )

lazy val scalaHttpMockZio = (project in file("modules/zio"))
  .settings(
    commonSettings,
    libraryDependencies ++= List(
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-interop-cats" % "3.3.0",
      "com.softwaremill.sttp.client3" %% "zio" % sttpVersion % Test,
      "dev.zio" %% "zio-test-sbt" % zioVersion % Test
    ),
    testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
  )
  .dependsOn(scalaHttpMockCore % "test->test;compile->compile")
