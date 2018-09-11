package org.scalahttpmock.expectation

import org.scalahttpmock.BaseTest
import org.scalahttpmock.expectation.matcher._

class AllMatchResultTest extends BaseTest {

  "prettify score with failed" should {

    "show failure where all single items have an empty score" in {
      val actual = createAnyAllMatchResult.prettifyScoreWithFailedOverview
      actual shouldBe
        """
        |[INVALID] SCORE:0.0/0.0 failed {METHOD Any, URI Any, CONTENT Any}
        |  Non matching headers : None
        |  Non matching params  : None
      """.stripMargin.trim

    }

    "show failure where all single items have an empty score and also multi items have a single empty score" in {
      val allMatchResult: AllMatchResult = createAnyAllMatchResult.copy(
        headerMatchResults =
          Vector(HeaderMatchResult(HeaderMatches("header_name", ".*".r), MatchingScore.empty)),
        paramMatchResults =
          Vector(ParamMatchResult(ParamEquals("param_name", "param_value"), MatchingScore.empty))
      )

      val actual = allMatchResult.prettifyScoreWithFailedOverview
      actual shouldBe
        """
          |[INVALID] SCORE:0.0/0.0 failed {METHOD Any, HEADERS (failed 1), URI Any, PARAMS (failed 1), CONTENT Any}
          |  Non matching headers : [ Match("header_name", ".*") ]
          |  Non matching params  : [ Equals("param_name", "param_value") ]
        """.stripMargin.trim
    }

    "show failure where all single items have an empty score and also multi items have a multiple empty scores" in {
      val allMatchResult: AllMatchResult = createAnyAllMatchResult.copy(
        headerMatchResults = Vector(
          HeaderMatchResult(HeaderMatches("header_name1", ".*a".r), MatchingScore.empty),
          HeaderMatchResult(HeaderMatches("header_name2", ".*b".r), MatchingScore.empty)
        ),
        paramMatchResults = Vector(
          ParamMatchResult(ParamEquals("param_name1", "param_value1"), MatchingScore.empty),
          ParamMatchResult(ParamEquals("param_name2", "param_value2"), MatchingScore.empty)
        )
      )

      val actual = allMatchResult.prettifyScoreWithFailedOverview
      actual shouldBe
        """
          |[INVALID] SCORE:0.0/0.0 failed {METHOD Any, HEADERS (failed 2), URI Any, PARAMS (failed 2), CONTENT Any}
          |  Non matching headers : [ Match("header_name1", ".*a"),
          |                           Match("header_name2", ".*b") ]
          |  Non matching params  : [ Equals("param_name1", "param_value1"),
          |                           Equals("param_name2", "param_value2") ]
        """.stripMargin.trim
    }

    "show success when they all match" in {
      val allMatchResult: AllMatchResult = createAnyAllMatchResult.copy(
        headerMatchResults =
          Vector(HeaderMatchResult(HeaderMatches("header_name", ".*".r), MatchingScore(1, 1))),
        httpMethodMatchResult = HttpMethodMatchResult(GetMatcher, MatchingScore(1, 1)),
        uriMatchResult = UriMatchResult(AnyUriMatcher, MatchingScore(1, 1)),
        paramMatchResults =
          Vector(ParamMatchResult(ParamEquals("param_name", ""), MatchingScore(1, 1))),
        contentMatchResult = ContentMatchResult(AnyContentMatcher, MatchingScore(1, 1))
      )

      allMatchResult.prettifyScoreWithFailedOverview shouldBe
        "[SUCCESS] SCORE:5.0/5.0".trim

    }

  }
}
