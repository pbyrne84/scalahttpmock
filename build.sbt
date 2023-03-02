organization := "com.github.pbyrne84"

name := "scalahttpmock"

version := "0.1-SNAPSHOT"

scalaVersion := "2.13.8"

val circeVersion = "0.14.2"

val zioVersion: String = "2.0.7"
val sttpVersion: String = "3.8.11"

// even though free port is detected it can still race across tests
Test / parallelExecution := false

libraryDependencies ++= Seq(
  // Optional for auto-derivation of JSON codecs
  "io.circe" %% "circe-generic" % circeVersion,
  // Optional for string interpolation to JSON model
  "io.circe" %% "circe-literal" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",
  "io.netty" % "netty-all" % "4.1.89.Final",
  "com.softwaremill.sttp.client3" %% "core" % sttpVersion % Test,
  "org.mockito" % "mockito-core" % "4.6.1" % Test,
  "dev.zio" %% "zio" % zioVersion % Test,
  "dev.zio" %% "zio-interop-cats" % "3.3.0" % Test,
  "org.typelevel" %% "cats-effect-testing-scalatest" % "1.5.0" % Test,
  "org.typelevel" %% "cats-effect" % "3.3.14" % Test,
  "dev.zio" %% "zio-test" % zioVersion % Test,
  "com.softwaremill.sttp.client3" %% "zio" % sttpVersion % Test,
  "com.softwaremill.sttp.client3" %% "cats" % sttpVersion % Test,
  "org.scalatest" %% "scalatest" % "3.2.13" % Test
)

Test / test := (Test / test)
  .dependsOn(Compile / scalafmtCheck)
  .dependsOn(Test / scalafmtCheck)
  .value

//not to be used in ci, intellij has got a bit bumpy in the format on save
val formatAndTest =
  taskKey[Unit]("format all code then run tests, do not use on CI as any changes will not be committed")

formatAndTest := {
  (Test / test)
    .dependsOn(Compile / scalafmtAll)
    .dependsOn(Test / scalafmtAll)
}.value
