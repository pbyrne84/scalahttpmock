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
  "org.eclipse.jetty" % "jetty-server" % "11.0.11",
  "org.eclipse.jetty" % "jetty-webapp" % "11.0.11",
  "com.softwaremill.sttp.client3" %% "core" % "3.7.2" % Test,
  "org.mockito" % "mockito-core" % "4.6.1" % Test,
  "dev.zio" %% "zio" % "2.0.2" % Test,
  "org.typelevel" %% "cats-effect" % "3.3.14" % Test
)

libraryDependencies += "org.scalatest" %% "scalatest" % "3.2.13" % Test

libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"

libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5"
