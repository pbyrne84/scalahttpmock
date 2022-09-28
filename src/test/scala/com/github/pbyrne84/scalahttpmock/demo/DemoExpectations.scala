package com.github.pbyrne84.scalahttpmock.demo

import com.github.pbyrne84.scalahttpmock.expectation.matcher.UriEquals
import com.github.pbyrne84.scalahttpmock.expectation.{
  JsonResponse,
  LocationResponse,
  ServiceExpectation
}
object DemoExpectations {

  val expectations: List[ServiceExpectation] = {
    List(
      ServiceExpectation(uriMatcher = UriEquals("/banana"))
        .withResponse(JsonResponse(404, Some("{}"), List.empty)),
      ServiceExpectation(uriMatcher = UriEquals("/apple"))
        .withResponse(LocationResponse(301, "https://www.google.com")),
      ServiceExpectation(uriMatcher = UriEquals("/favicon.ico"))
        .withResponse(JsonResponse(404))
    )
  }
}
