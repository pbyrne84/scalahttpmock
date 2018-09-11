package com.github.pbyrne84.scalahttpmock.expectation

import cats.effect.IO
import org.http4s
import org.http4s.{Headers, Request}

object RequestPrettification {
  private val prettification = new RequestPrettification

  implicit class RequestPrettify(request: Request[IO]) {

    def prettyFormat: String = prettification.prettify(request)
  }
}

class RequestPrettification extends Indentation {
  private val multiEntryFormat = """("%s", "%s")"""

  def prettify(request: Request[IO]): String =
    s"""
     |Request[method="${request.method}", path="${request.pathInfo}"](
     |  Uri     : "${request.uri}",
     |  Params  : ${indentNewLines(14, formatParams(request.multiParams))},
     |  Headers : ${indentNewLines(14, formatHeaders(request.headers))},
     |  Body    : ${getBody(request).getOrElse("None")}
     |)
     |""".stripMargin.trim

  private def formatParams(multiParams: Map[String, scala.Seq[String]]): String = {
    if (multiParams.isEmpty) {
      "[]"
    } else {
      val convertedEntries = multiParams.toList.sortBy(_._1).flatMap {
        case (name: String, entries: scala.Seq[String]) =>
          if (entries.isEmpty) {
            List(multiEntryFormat.format(name, ""))
          } else {
            entries.map(value => multiEntryFormat.format(name, value))
          }
      }

      s"""[ ${convertedEntries.mkString(",\n")} ]"""
    }
  }

  private def formatHeaders(headers: Headers): String = {
    if (headers.isEmpty) {
      "[]"
    } else {
      val convertedEntries = headers.toList
        .map(header => header.name.value -> header.value)
        .sortBy(_._1)
        .map {
          case (name, value) =>
            multiEntryFormat.format(name, value)
        }

      s"""[ ${convertedEntries.mkString(",\n")} ]"""
    }
  }

  private def getBody[F](request: Request[IO]): Option[String] = {
    if (request.body == http4s.EmptyBody) {
      None
    } else {
      Some(request.as[String].unsafeRunSync)
    }
  }
}
