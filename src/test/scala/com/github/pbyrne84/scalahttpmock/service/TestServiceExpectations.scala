package com.github.pbyrne84.scalahttpmock.service

import com.github.pbyrne84.scalahttpmock.expectation.matcher.{GetMatcher, HeaderEquals, HttpMethodMatcher, PostMatcher}
import com.github.pbyrne84.scalahttpmock.expectation.{Header, JsonResponse, LocationResponse, ServiceExpectation}

/** This does make the tests more obscure but the test cases need to be triplicated. Future, IO and ZIO need to be
  * covered.
  */
class TestServiceExpectations {
  import com.github.pbyrne84.scalahttpmock.testextensions.TestStringOps._

  val successTestPath: String = "/test/path"
  val setupHeader: (String, String) = "a" -> "value"

  object nonMatching {
    val implementation: ServiceExpectation =
      ServiceExpectation()
        .addHeader(HeaderEquals(setupHeader._1, setupHeader._2))
        .withMethod(GetMatcher)
        .withUri("/test/anotherpath".asUriEquals)
        .withResponse(JsonResponse(202))
  }

  object matchingGet {
    def implementation(responseJson: String, customResponseHeader: Header): ServiceExpectation =
      ServiceExpectation()
        .addHeader(HeaderEquals(setupHeader._1, setupHeader._2))
        .withMethod(GetMatcher)
        .withUri(successTestPath.asUriEquals)
        .withResponse(JsonResponse(202, Some(responseJson), Vector(customResponseHeader)))
  }

  object matchingPost {
    def implementation(postMatcher: PostMatcher, responseJson: String, customHeader: Header): ServiceExpectation =
      ServiceExpectation()
        .addHeader(HeaderEquals(setupHeader._1, setupHeader._2))
        .withMethod(postMatcher)
        .withUri(successTestPath.asUriEquals)
        .withResponse(JsonResponse(202, Some(responseJson), Vector(customHeader)))
  }

  object matchingGetLocationRedirect {

    def implementation(calledUrl: String, redirectUrl: String): ServiceExpectation =
      ServiceExpectation()
        .withUri(calledUrl.asUriEquals)
        .withMethod(HttpMethodMatcher.getMatcher)
        .withResponse(LocationResponse(303, redirectUrl))
  }

}
