organization := "com.github.pbyrne84"

name := "scalahttpmock"

version := "0.1-SNAPSHOT"

scalaVersion := "2.13.8"

val http4sVersion = "0.21.0"
val circeVersion  = "0.13.0"

libraryDependencies ++= Seq(
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,
  "org.http4s" %% "http4s-circe" % http4sVersion,
  // Optional for auto-derivation of JSON codecs
  "io.circe" %% "circe-generic" % circeVersion,
  // Optional for string interpolation to JSON model
  "io.circe" %% "circe-literal" % circeVersion,
  "io.circe" %% "circe-parser" % circeVersion,
  "org.eclipse.jetty" % "jetty-server" % "11.0.11",
  "org.eclipse.jetty" % "jetty-webapp" % "11.0.11",
  "com.softwaremill.sttp" %% "core" % "1.6.0" % Test,
)

libraryDependencies += "org.scalatest" %% "scalatest" % "3.1.0" % Test

libraryDependencies += "org.scalamock" %% "scalamock" % "4.3.0" % Test

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"

libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2"

libraryDependencies += "org.clapper" %% "classutil" % "1.5.0" % Provided
