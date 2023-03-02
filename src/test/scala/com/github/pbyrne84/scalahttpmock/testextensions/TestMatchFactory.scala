package com.github.pbyrne84.scalahttpmock.testextensions

import com.github.pbyrne84.scalahttpmock.expectation.MockHttpMethod.GET
import com.github.pbyrne84.scalahttpmock.expectation._
import com.github.pbyrne84.scalahttpmock.expectation.matcher.{AnyContentMatcher, AnyHttpMethodMatcher, AnyUriMatcher}
import com.github.pbyrne84.scalahttpmock.service.URI

class TestMatchFactory {
  def createAnyAllMatchResult: AllMatchResult = {
    val httpMethodResult = HttpMethodMatchResult(AnyHttpMethodMatcher, MatchingScore.empty)
    val uriMatchResult = UriMatchResult(AnyUriMatcher, MatchingScore.empty)
    val contentMatchResult = ContentMatchResult(AnyContentMatcher, MatchingScore.empty)

    AllMatchResult(Vector(), httpMethodResult, uriMatchResult, Vector(), contentMatchResult)
  }

  def createSuccessfulMatchResult(total: Int): AllMatchResult = createAnyAllMatchResult.copy(
    uriMatchResult = UriMatchResult(AnyUriMatcher, MatchingScore.success(total))
  )

  def createMatchResultWithScore(total: Double, possible: Double): AllMatchResult =
    createAnyAllMatchResult.copy(
      uriMatchResult = UriMatchResult(AnyUriMatcher, MatchingScore(total, possible))
    )

  def createRequest: MatchableRequest = {
    val uri = URI("/")

    MatchableRequest(uri.path, uri.pathWithParams, List.empty, GET, Map.empty, None, uri.uri)
  }

}
