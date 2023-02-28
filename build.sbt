organization := "com.github.pbyrne84"

name := "scalahttpmock"

version := "0.1-SNAPSHOT"

scalaVersion := "2.13.8"

val circeVersion = "0.14.2"

libraryDependencies ++= Seq(
  // Optional for auto-derivation of JSON codecs
  "io.circe" %% "circe-generic" % circeVersion,
  // Optional for string interpolation to JSON model
  "io.circe" %% "circe-literal" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "io.netty" % "netty-all" % "4.1.89.Final",
  "com.softwaremill.sttp.client3" %% "core" % "3.7.2" % Test,
  "org.mockito" % "mockito-core" % "4.6.1" % Test,
  "dev.zio" %% "zio" % "2.0.2" % Test,
  "org.typelevel" %% "cats-effect" % "3.3.14" % Test
)

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.13" % Test

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"

libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5"

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
