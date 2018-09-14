package com.github.pbyrne84.scalahttpmock.service.request

import com.github.pbyrne84.scalahttpmock.BaseSpec

import scala.collection.immutable.Seq

class UnSuccessfulResponseSpec extends BaseSpec {
  "prettyText" should {

    "format request with no expectations (match attempts)" in {
      val actual = UnSuccessfulResponse(createRequest, Seq.empty).prettyFormat

      actual shouldBe
        """
        |The Request was not matched :-
        |  Request[method="GET", path="/"](
        |    Uri     : "/",
        |    Params  : [],
        |    Headers : [],
        |    Body    : None
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
        |    Uri     : "/",
        |    Params  : [],
        |    Headers : [],
        |    Body    : None
        |  )
        |  There was 1 expectations :-
        |    [INVALID] SCORE:0.0/0.0 failed {METHOD Any, URI Any, CONTENT Any}
        |      Non matching headers : None
        |      Non matching params  : None
      """.stripMargin.trim
    }

    "format request with multiple expectations (match attempts) sorting by highest score" in {
      val allAttempts = List(createMatchResultWithScore(8, 11), createMatchResultWithScore(8, 10))
      val actual = UnSuccessfulResponse(createRequest, allAttempts).prettyFormat

      actual shouldBe
        """
          |The Request was not matched :-
          |  Request[method="GET", path="/"](
          |    Uri     : "/",
          |    Params  : [],
          |    Headers : [],
          |    Body    : None
          |  )
          |  There was 2 expectations :-
          |    [INVALID] SCORE:10.0/8.0 failed {METHOD Any, URI Any, CONTENT Any}
          |      Non matching headers : None
          |      Non matching params  : None
          |
          |    [INVALID] SCORE:11.0/8.0 failed {METHOD Any, URI Any, CONTENT Any}
          |      Non matching headers : None
          |      Non matching params  : None
      """.stripMargin.trim
    }

  }
}
