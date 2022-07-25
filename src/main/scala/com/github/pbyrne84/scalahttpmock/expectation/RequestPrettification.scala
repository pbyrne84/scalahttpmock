package com.github.pbyrne84.scalahttpmock.expectation

import org.http4s.Header

object RequestPrettification {
  private val prettification = new RequestPrettification

  implicit class RequestPrettify(request: MatchableRequest) {

    def prettyFormat: String = prettification.prettify(request)
  }
}

class RequestPrettification extends Indentation {
  private val multiEntryFormat = """("%s", "%s")"""

  def prettify(request: MatchableRequest): String =
    s"""
       |Request[method="${request.method}", path="${request.uriPath}"](
       |  Uri            : "${request.uri}",
       |  PathWithParams : "${request.asPathWithParams}",
       |  Params         : ${indentNewLines(21, formatParams(request.multiParams))},
       |  Headers        : ${indentNewLines(21, formatHeaders(request.headersList))},
       |  Body           : ${request.maybeContentAsString}
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

  private def formatHeaders(headers: List[Header]): String = {
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

}
