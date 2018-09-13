organization := "com.github.pbyrne84"

name := "scalahttpmock"

version := "0.1-SNAPSHOT"

scalaVersion := "2.12.6"

val http4sVersion = "0.18.11"

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion,
  // Optional for auto-derivation of JSON codecs
  "io.circe" %% "circe-generic" % "0.9.3",
  // Optional for string interpolation to JSON model
  "io.circe" %% "circe-literal" % "0.9.3",
  "io.circe" %% "circe-parser" % "0.9.3",
  "com.softwaremill.sttp" %% "core" % "1.3.1" % Test
)

libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.5" % Test

libraryDependencies += "org.scalamock" %% "scalamock-scalatest-support" % "3.6.0" % Test

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"

libraryDependencies +=  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0"

libraryDependencies += "org.clapper" %% "classutil" % "1.3.0" % Provided
