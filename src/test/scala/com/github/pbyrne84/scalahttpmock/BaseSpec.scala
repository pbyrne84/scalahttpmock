package com.github.pbyrne84.scalahttpmock

import cats.effect.IO
import com.github.pbyrne84.scalahttpmock.expectation.matcher._
import com.github.pbyrne84.scalahttpmock.expectation.{
  AllMatchResult,
  ContentMatchResult,
  HttpMethodMatchResult,
  MatchableRequest,
  MatchingScore,
  UriMatchResult
}
import org.http4s.{Header, Request, Uri}
import org.scalamock.scalatest.MockFactory
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers

import scala.language.implicitConversions

class BaseSpec extends AnyWordSpec with Matchers with MockFactory {

  implicit class StringAsMatcher(string: String) {
    def asUri: Uri = Uri.unsafeFromString(string)
    def asPathEquals: UriMatcher = PathEquals(string)
    def asPathMatches: UriMatcher = PathMatches(string.r)
    def asUriEquals: UriMatcher = UriEquals(string)
    def asUriMatches: UriMatcher = UriMatches(string.r)
    def asJsonPostMatcher: PostMatcher = PostMatcher(JsonContentEquals(string))
  }

  implicit class Tuple2AsParamMatcher(tuple: (String, String)) {
    private val (name, matcherText) = tuple

    def asParamEquals: ParamEquals = ParamEquals(name, matcherText)
    def asParamMatches: ParamMatches = ParamMatches(name, matcherText.r)
  }

  implicit class Tuple2AsHeaderMatcher(tuple: (String, String)) {
    private val (name, matcherText) = tuple

    def asHeaderEquals: HeaderMatcher = HeaderEquals(name, matcherText)
    def asHeaderMatches: HeaderMatcher = HeaderMatches(name, matcherText.r)
  }

  implicit class Tuple2sAsParamMatchers(tuples: Seq[(String, String)]) {
    def asParamEquals: Seq[ParamEquals] = tuples.map(_.asParamEquals)
  }

  implicit class Tuple2sAsAssertion(tuples: Seq[(String, String)]) {

    def shouldHaveEntry(expected: (String, String)): Unit = {
      tuples.find(_._1 == expected._1) shouldBe Some(expected._1, expected._2)
    }

    def shouldHaveEntry(expected: Header): Unit = {
      shouldHaveEntry(expected.name.value, expected.value)
    }

  }

  implicit class RequestIOOps(http4sRequest: Request[IO]) {
    val asMatchable: MatchableRequest = MatchableRequest.fromRequestIO(http4sRequest)
  }

  protected def createRequest: Request[IO] = Request()

  protected def createAnyAllMatchResult: AllMatchResult = {
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

}
