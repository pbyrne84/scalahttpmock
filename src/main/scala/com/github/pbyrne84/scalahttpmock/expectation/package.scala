package com.github.pbyrne84.scalahttpmock
import org.eclipse.jetty.server.Request

package object expectation {

  implicit class RequestOps(request: Request) {
    def asPathWithParams: String = {
      val path = request.getPathInfo
      Option(request.getQueryString)
        .map { queryString =>
          s"$path?$queryString"
        }
        .getOrElse(path)
    }
  }
}
