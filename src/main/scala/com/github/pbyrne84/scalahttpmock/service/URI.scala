package com.github.pbyrne84.scalahttpmock.service

object URI {
  def apply(uri: String): URI = {
    val protocolDomainRegex = "(\\w+)://([\\w\\.]+)((:\\d+)?)"

    val pathWithQueryString = uri.replaceAll(protocolDomainRegex, "")
    val parts = pathWithQueryString.split("\\?").toList
    val (path, maybeParamString, paramMap) = calculatePathAndParams(parts)

    val pathWithParams = maybeParamString
      .map(params => path + "?" + params)
      .getOrElse(path)

    URI(uri, path, pathWithParams, maybeParamString, paramMap)
  }

  private def calculatePathAndParams(
      parts: List[String]
  ): (String, Option[String], Map[String, List[String]]) = {
    def padPath(path: String): String = {
      if (path.nonEmpty) {
        path
      } else {
        path + "/"
      }
    }

    parts match {
      case path :: paramString :: _ =>
        val paramParts = paramString
          .split("&")
          .toList

        val paramMap = paramParts
          .map { param =>
            val paramValueParts = param.split("=").toList

            paramValueParts match {
              case name :: value :: _ => (name, value)
              case name :: _ => (name, "")
              case _ => ("", "")
            }
          }
          .groupBy(_._1)
          .map {
            case (name, paramNamesWithValues) =>
              name -> paramNamesWithValues.map(_._2)
          }

        (padPath(path), Some(paramString), paramMap)

      case path :: _ =>
        (padPath(path), None, Map.empty)

      case Nil => ("/", None, Map.empty)
    }

  }
}
case class URI(uri: String,
               path: String,
               pathWithParams: String,
               maybeQuery: Option[String],
               params: Map[String, List[String]])
