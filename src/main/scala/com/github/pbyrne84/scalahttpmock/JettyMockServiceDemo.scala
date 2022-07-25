package com.github.pbyrne84.scalahttpmock

import com.github.pbyrne84.scalahttpmock.expectation.matcher.UriEquals
import com.github.pbyrne84.scalahttpmock.expectation.{
  JsonResponse,
  LocationResponse,
  ServiceExpectation
}
import com.github.pbyrne84.scalahttpmock.service.JettyMockService

object JettyMockServiceDemo {

  def main(args: Array[String]): Unit = {
    val jettyMockService: JettyMockService = JettyMockService.create(8080)

    println("banana " + jettyMockService.server.getThreadPool.getThreads)
    val response: JsonResponse = JsonResponse(404, Some("{}"), List.empty)
    jettyMockService
      .addExpectation(
        ServiceExpectation(uriMatcher = UriEquals("/banana")).withResponse(response)
      )
      .addExpectation(
        ServiceExpectation(uriMatcher = UriEquals("/apple"))
          .withResponse(LocationResponse(301, "https://www.google.com"))
      )

    jettyMockService.server.setStopAtShutdown(true)
  }

}
