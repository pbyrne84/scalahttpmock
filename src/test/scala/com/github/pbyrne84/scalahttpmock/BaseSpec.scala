package com.github.pbyrne84.scalahttpmock

import com.github.pbyrne84.scalahttpmock.expectation.Method.GET
import com.github.pbyrne84.scalahttpmock.expectation.matcher._
import com.github.pbyrne84.scalahttpmock.expectation._
import com.github.pbyrne84.scalahttpmock.prettifier.CaseClassPrettifier
import com.github.pbyrne84.scalahttpmock.service.URI
import org.scalactic.Prettifier
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import sttp.model

import scala.language.implicitConversions

abstract class BaseSpec extends AnyWordSpec with Matchers {

  implicit val prettifier: Prettifier = Prettifier.apply {
    case a: AnyRef if CaseClassPrettifier.shouldBeUsedInTestMatching(a) =>
      new CaseClassPrettifier().prettify(a)

    case a: Any =>
      Prettifier.default(a)

  }

  implicit class StringAsMatcher(string: String) {
    def asUri: URI = URI(string)
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

  implicit class SttpHeadersAsAssertion(tuples: Seq[model.Header]) {

    def shouldHaveEntry(expected: (String, String)): Unit = {
      tuples.find(_.name == expected._1) shouldBe Some(model.Header(expected._1, expected._2))
    }

    def shouldHaveEntry(expected: Header): Unit = {
      shouldHaveEntry(expected.name.value, expected.value)
    }
  }

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

  def createRequest: MatchableRequest = {
    val uri = URI("/")

    MatchableRequest(uri.path, uri.pathWithParams, List.empty, GET, Map.empty, None, uri.uri)
  }

  implicit class MatchableRequestOps(matchableRequest: MatchableRequest) {
    def withUri(uriString: String): MatchableRequest = {
      val uri = URI(uriString)

      val uriPath = uri.path

      matchableRequest.copy(
        uriPath = uriPath,
        uri = uri.uri,
        asPathWithParams = uri.pathWithParams,
        multiParams = uri.params
      )

    }

    def withMethod(method: Method): MatchableRequest = {
      matchableRequest.copy(method = method)
    }

    def withBody(body: String): MatchableRequest = {
      val headers = List(
        Header("Content-Length", body.length.toString),
        Header("Content-Type", "text/plain; charset=UTF-8")
      )
      matchableRequest.copy(
        maybeContentAsString = Some(body),
        headers = headers
      )
    }

    def withHeaders(headers: List[Header]): MatchableRequest = {
      matchableRequest.copy(headers = headers)
    }
  }

}
