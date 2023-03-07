package com.github.pbyrne84.scalahttpmock.expectation

import com.github.pbyrne84.scalahttpmock.expectation.matcher._
import com.github.pbyrne84.scalahttpmock.shared.BaseSpec

class AllMatchResultSpec extends BaseSpec {

  import com.github.pbyrne84.scalahttpmock.testextensions.TestStringOps._

  "prettify result to be " should {
    "show failure where all single items have an empty score" in {
      val actual = testMatchFactory.createAnyAllMatchResult.prettifyResult
      actual shouldBe
        """
            |[INVALID] SCORE:0.0/0.0 failed {METHOD Any, URI Any, CONTENT Any}
            |  Method  - Non matching : Any
            |  Headers - Matching     : None
            |  Headers - Non matching : None
            |  Uri     - Non matching : Any
            |  Params  - Matching     : None
            |  Params  - Non matching : None
            |  Content - Non matching : Any
          """.stripMargin.trim
    }

    "show http method success" in {
      val actual = testMatchFactory.createAnyAllMatchResult
        .copy(
          httpMethodMatchResult = HttpMethodMatchResult(AnyHttpMethodMatcher, MatchingScore.success(10))
        )
        .prettifyResult
      actual shouldBe
        """
            |[INVALID] SCORE:10.0/10.0 failed {URI Any, CONTENT Any}
            |  Method  - Matching     : Any
            |  Headers - Matching     : None
            |  Headers - Non matching : None
            |  Uri     - Non matching : Any
            |  Params  - Matching     : None
            |  Params  - Non matching : None
            |  Content - Non matching : Any
          """.stripMargin.trim
    }

    "show failed headers with successful headers" in {
      val allMatchResult: AllMatchResult = testMatchFactory.createAnyAllMatchResult.copy(
        headerMatchResults = Vector(
          HeaderMatchResult(HeaderMatches("header_name1", ".*a".r), MatchingScore.success(10)),
          HeaderMatchResult(HeaderMatches("header_name2", ".*b".r), MatchingScore.success(10)),
          HeaderMatchResult(HeaderMatches("header_name3", ".*c".r), MatchingScore.fail(10)),
          HeaderMatchResult(HeaderMatches("header_name4", ".*d".r), MatchingScore.fail(10))
        )
      )
      val actual = allMatchResult.prettifyResult

      actual shouldBe
        """
            |[INVALID] SCORE:40.0/20.0 failed {METHOD Any, HEADERS (failed 2), URI Any, CONTENT Any}
            |  Method  - Non matching : Any
            |  Headers - Matching     : [ Match("header_name1", ".*a"),
            |                             Match("header_name2", ".*b") ]
            |  Headers - Non matching : [ Match("header_name3", ".*c"),
            |                             Match("header_name4", ".*d") ]
            |  Uri     - Non matching : Any
            |  Params  - Matching     : None
            |  Params  - Non matching : None
            |  Content - Non matching : Any
          """.stripMargin.trim
    }

    "show successful uri" in {
      val allMatchResult: AllMatchResult = testMatchFactory.createAnyAllMatchResult.copy(
        uriMatchResult = UriMatchResult("http://x/z/b".asUriEquals, MatchingScore.success(10))
      )
      val actual = allMatchResult.prettifyResult

      actual shouldBe
        """
            |[INVALID] SCORE:10.0/10.0 failed {METHOD Any, CONTENT Any}
            |  Method  - Non matching : Any
            |  Headers - Matching     : None
            |  Headers - Non matching : None
            |  Uri     - Matching     : Uri equals "http://x/z/b"
            |  Params  - Matching     : None
            |  Params  - Non matching : None
            |  Content - Non matching : Any
          """.stripMargin.trim
    }

    "show failed params with successful params" in {
      val allMatchResult: AllMatchResult = testMatchFactory.createAnyAllMatchResult.copy(
        paramMatchResults = Vector(
          ParamMatchResult(ParamMatches("param_name1", ".*a".r), MatchingScore.success(10)),
          ParamMatchResult(ParamMatches("param_name2", ".*b".r), MatchingScore.success(10)),
          ParamMatchResult(ParamMatches("param_name3", ".*c".r), MatchingScore.fail(10)),
          ParamMatchResult(ParamMatches("param_name4", ".*d".r), MatchingScore.fail(10))
        )
      )
      val actual = allMatchResult.prettifyResult

      actual shouldBe
        """
            |[INVALID] SCORE:40.0/20.0 failed {METHOD Any, URI Any, PARAMS (failed 2), CONTENT Any}
            |  Method  - Non matching : Any
            |  Headers - Matching     : None
            |  Headers - Non matching : None
            |  Uri     - Non matching : Any
            |  Params  - Matching     : [ Match("param_name1", ".*a"),
            |                             Match("param_name2", ".*b") ]
            |  Params  - Non matching : [ Match("param_name3", ".*c"),
            |                             Match("param_name4", ".*d") ]
            |  Content - Non matching : Any
          """.stripMargin.trim
    }

    "show non matching json content" in {
      val json =
        """
          |{
          | "a" : "1"
          |}
        """.stripMargin

      val actual = testMatchFactory.createAnyAllMatchResult
        .copy(
          contentMatchResult = ContentMatchResult(JsonContentEquals(json), MatchingScore.fail(10))
        )
        .prettifyResult

      actual shouldBe
        """
            |[INVALID] SCORE:10.0/0.0 failed {METHOD Any, URI Any, CONTENT Equals Json}
            |  Method  - Non matching : Any
            |  Headers - Matching     : None
            |  Headers - Non matching : None
            |  Uri     - Non matching : Any
            |  Params  - Matching     : None
            |  Params  - Non matching : None
            |  Content - Non matching : {
            |    "a" : "1"
            |  }
          """.stripMargin.trim
    }

    "show matching json content" in {
      val json =
        """
          |{
          | "a" : "1"
          |}
        """.stripMargin

      val actual = testMatchFactory.createAnyAllMatchResult
        .copy(
          contentMatchResult = ContentMatchResult(JsonContentEquals(json), MatchingScore.success(10))
        )
        .prettifyResult

      actual shouldBe
        """
            |[INVALID] SCORE:10.0/10.0 failed {METHOD Any, URI Any}
            |  Method  - Non matching : Any
            |  Headers - Matching     : None
            |  Headers - Non matching : None
            |  Uri     - Non matching : Any
            |  Params  - Matching     : None
            |  Params  - Non matching : None
            |  Content - Matching     : {
            |    "a" : "1"
            |  }
          """.stripMargin.trim
    }
  }

  "show success when they all match" in {
    val allMatchResult: AllMatchResult = testMatchFactory.createAnyAllMatchResult.copy(
      headerMatchResults = Vector(HeaderMatchResult(HeaderMatches("header_name", ".*".r), MatchingScore(1, 1))),
      httpMethodMatchResult = HttpMethodMatchResult(GetMatcher, MatchingScore(1, 1)),
      uriMatchResult = UriMatchResult(AnyUriMatcher, MatchingScore(1, 1)),
      paramMatchResults = Vector(ParamMatchResult(ParamEquals("param_name", ""), MatchingScore(1, 1))),
      contentMatchResult = ContentMatchResult(AnyContentMatcher, MatchingScore(1, 1))
    )

    allMatchResult.prettifyResult shouldBe
      "[SUCCESS] SCORE:5.0/5.0".trim

  }

}
