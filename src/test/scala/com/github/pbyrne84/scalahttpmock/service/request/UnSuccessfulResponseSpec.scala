package com.github.pbyrne84.scalahttpmock.service.request

import com.github.pbyrne84.scalahttpmock.BaseSpec

class UnSuccessfulResponseSpec extends BaseSpec {
  "prettyText" should {

    "format request with no expectations (match attempts)" in {
      val actual = UnSuccessfulResponse(createRequest, Seq.empty).prettyFormat

      actual shouldBe
        """
          |The Request was not matched :-
          |  Request[method="GET", path="/"](
          |    Uri            : "/",
          |    PathWithParams : "/",
          |    Params         : [],
          |    Headers        : [],
          |    Body           : None
          |  )
          |  There was 0 expectations :-
      """.stripMargin.trim
    }

    "format request with expectation (match attempt)" in {
      val allAttempts = List(createAnyAllMatchResult)
      val actual = UnSuccessfulResponse(createRequest, allAttempts).prettyFormat

      actual shouldBe
        """
          |The Request was not matched :-
          |  Request[method="GET", path="/"](
          |    Uri            : "/",
          |    PathWithParams : "/",
          |    Params         : [],
          |    Headers        : [],
          |    Body           : None
          |  )
          |  There was 1 expectations :-
          |    [INVALID] SCORE:0.0/0.0 failed {METHOD Any, URI Any, CONTENT Any}
          |      Method  - Non matching : Any
          |      Headers - Matching     : None
          |      Headers - Non matching : None
          |      Uri     - Non matching : Any
          |      Params  - Matching     : None
          |      Params  - Non matching : None
          |      Content - Non matching : Any
      """.stripMargin.trim
    }

    "format request with multiple expectations (match attempts) sorting by highest score" in {
      val allAttempts = List(createMatchResultWithScore(8, 11), createMatchResultWithScore(8, 10))
      val actual = UnSuccessfulResponse(createRequest, allAttempts).prettyFormat

      actual shouldBe
        """
          |The Request was not matched :-
          |  Request[method="GET", path="/"](
          |    Uri            : "/",
          |    PathWithParams : "/",
          |    Params         : [],
          |    Headers        : [],
          |    Body           : None
          |  )
          |  There was 2 expectations :-
          |    [INVALID] SCORE:10.0/8.0 failed {METHOD Any, URI Any, CONTENT Any}
          |      Method  - Non matching : Any
          |      Headers - Matching     : None
          |      Headers - Non matching : None
          |      Uri     - Non matching : Any
          |      Params  - Matching     : None
          |      Params  - Non matching : None
          |      Content - Non matching : Any
          |
          |    [INVALID] SCORE:11.0/8.0 failed {METHOD Any, URI Any, CONTENT Any}
          |      Method  - Non matching : Any
          |      Headers - Matching     : None
          |      Headers - Non matching : None
          |      Uri     - Non matching : Any
          |      Params  - Matching     : None
          |      Params  - Non matching : None
          |      Content - Non matching : Any
      """.stripMargin.trim
    }

  }
}
