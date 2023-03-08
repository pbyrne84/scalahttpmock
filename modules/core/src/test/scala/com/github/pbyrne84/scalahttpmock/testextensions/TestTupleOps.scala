package com.github.pbyrne84.scalahttpmock.testextensions

import com.github.pbyrne84.scalahttpmock.expectation.matcher._
import com.github.pbyrne84.scalahttpmock.expectation.{CaseInsensitiveString, Header}
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import sttp.model

object TestTupleOps {
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

  implicit class SttpHeadersAsAssertion(headers: Seq[model.Header]) extends Matchers {

    def shouldHaveEntry(expected: (String, String)): Assertion = {
      // header case seems to auto changed to lowercase from what is actually mocked
      headers.find(header => CaseInsensitiveString(header.name) == CaseInsensitiveString(expected._1)) shouldBe Some(
        model.Header(expected._1, expected._2)
      )
    }

    def shouldHaveEntry(expected: Header): Assertion = {
      shouldHaveEntry(expected.name.value, expected.value)
    }

    def tupledContains(expected: (String, String)): Boolean = {
      headers.contains(model.Header(expected._1, expected._2))
    }

  }
}
