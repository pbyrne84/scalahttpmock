package com.github.pbyrne84.scalahttpmock.expectation
import com.github.pbyrne84.scalahttpmock.BaseSpec
import com.github.pbyrne84.scalahttpmock.expectation.matcher._
import org.scalatest.OptionValues
import org.scalatest.prop.TableDrivenPropertyChecks

class MatchingAttemptSpec extends BaseSpec with TableDrivenPropertyChecks with OptionValues {

  private val matchingAttempt = new MatchingAttempt

  "matching uri attempt" should {

    "match path equality excluding params" in {
      val examples = Table(
        ("success", "value"),
        (MatchingScore.success(1), "/xxxx/yyyy"),
        (MatchingScore(0, 1), "/xxxx/yyya")
      )

      forAll(examples) {
        case (matches, matcherText: String) =>
          val pathEquals = matcherText.asPathEquals
          matchingAttempt
            .tryMatching(
              ServiceExpectation(uriMatcher = pathEquals),
              createRequest.withUri("http://www.x.com/xxxx/yyyy?x=1&y=2")
            )
            .uriMatchResult shouldBe UriMatchResult(uriMatcher = pathEquals,
                                                    matchingScore = matches)
      }
    }

    "match path regex excluding params" in {
      val examples = Table(
        ("success", "value"),
        (MatchingScore.success(1), "/xxxx/(y){4}"),
        (MatchingScore(0, 1), "/xxxx/(y){5}")
      )
      forAll(examples) {
        case (score, matcherText: String) =>
          val pathMatches = matcherText.asPathMatches
          val expectation = ServiceExpectation(uriMatcher = pathMatches)

          matchingAttempt
            .tryMatching(
              expectation,
              createRequest.withUri("http://www.x.com/xxxx/yyyy?x=1&y=2")
            )
            .uriMatchResult shouldBe UriMatchResult(uriMatcher = pathMatches, matchingScore = score)
      }
    }

    "match uri with no params" in {
      val uriEquals = "/xxxx/yyyy".asUriEquals
      val expectation = ServiceExpectation(uriMatcher = uriEquals)

      matchingAttempt
        .tryMatching(
          expectation,
          createRequest.withUri("http://www.x.com/xxxx/yyyy")
        )
        .uriMatchResult shouldBe UriMatchResult(uriMatcher = uriEquals,
                                                matchingScore =
                                                  MatchingScore.success(uriEquals.maxScore))

    }

    "match uri with params" in {
      val examples = Table(
        ("success", "value"),
        (MatchingScore.success(1), "/xxxx/yyyy?x=1&y=2"),
        (MatchingScore(0, 1), "/xxxx/yyyy?x=1&y=3")
      )

      forAll(examples) { (matches, matcherText: String) =>
        val uriEquals = matcherText.asUriEquals
        val expectation = ServiceExpectation(uriMatcher = uriEquals)

        matchingAttempt
          .tryMatching(
            expectation,
            createRequest.withUri("http://www.x.com/xxxx/yyyy?x=1&y=2")
          )
          .uriMatchResult shouldBe UriMatchResult(uriMatcher = uriEquals, matchingScore = matches)
      }
    }

    "match uri with params using regex" in {
      val examples = Table(
        ("success", "value"),
        (MatchingScore.success(1), "/xxxx/(y){4}\\?x=1&y=(\\d)"),
        (MatchingScore(0, 1), "/xxxx/(y){4}\\?x=1&y=[^\\d]")
      )

      forAll(examples) { (matches, matcherText: String) =>
        val uriMatches = matcherText.asUriMatches
        val expectation = ServiceExpectation(uriMatcher = uriMatches)

        matchingAttempt
          .tryMatching(
            expectation,
            createRequest.withUri("http://www.x.com/xxxx/yyyy?x=1&y=2")
          )
          .uriMatchResult shouldBe UriMatchResult(uriMatcher = uriMatches, matchingScore = matches)
      }
    }
  }

  "matching params" should {
    "not care about order and allow a combination of matching and equality" in {
      val paramEqualsMatchers = Vector(
        ("y", "2"),
        ("x", "1")
      ).map(_.asParamEquals)

      val paramMatchesMatchers = Vector(
        ("z", "^(c|a|b){3}$"),
        ("a", "(.)")
      ).map(_.asParamMatches)

      val paramMatches = Vector(
        ParamMatchResult(paramEqualsMatchers(0), MatchingScore.success(1)),
        ParamMatchResult(paramEqualsMatchers(1), MatchingScore.success(1)),
        ParamMatchResult(paramMatchesMatchers(0), MatchingScore.success(1)),
        ParamMatchResult(paramMatchesMatchers(1), MatchingScore.success(1))
      )

      val expectation =
        ServiceExpectation(paramMatchers = paramEqualsMatchers ++ paramMatchesMatchers)

      matchingAttempt
        .tryMatching(
          expectation,
          createRequest.withUri("http://www.x.com/xxxx/yyyy?a=t&x=1&y=2&z=abc")
        )
        .paramMatchResults shouldBe paramMatches
    }

    "not care about order and allow a combination of matching and equality for params failing on either" in {
      val paramEqualsMatchers = Vector(
        ("y", "3"),
        ("x", "2")
      ).map(_.asParamEquals)

      val paramMatchesMatchers = Vector(
        ("z", "^(c|a|b){3}$"),
        ("a", "(.){2}")
      ).map(_.asParamMatches)

      val paramMatches = Vector(
        ParamMatchResult(paramEqualsMatchers(0), MatchingScore.fail(1)),
        ParamMatchResult(paramEqualsMatchers(1), MatchingScore.fail(1)),
        ParamMatchResult(paramMatchesMatchers(0), MatchingScore.fail(1)),
        ParamMatchResult(paramMatchesMatchers(1), MatchingScore.fail(1))
      )

      val expectation =
        ServiceExpectation(paramMatchers = paramEqualsMatchers ++ paramMatchesMatchers)

      matchingAttempt
        .tryMatching(
          expectation,
          createRequest.withUri("http://www.x.com/xxxx/yyyy?a=t&x=1&y=2&z=abcc")
        )
        .paramMatchResults shouldBe paramMatches
    }

  }

  "match http request method" should {
    val allVerbs: Vector[Method] =
      Vector(Method.GET, Method.POST, Method.PUT, Method.DELETE, Method.PATCH)

    "match any" in {
      val expectation = ServiceExpectation(httpMethodMatcher = AnyHttpMethodMatcher)

      val verbs = Table(
        "verb",
        allVerbs: _*
      )

      forAll(verbs) { verb =>
        val request = createRequest
          .withUri("http://www.x.com/xxxx/yyyy")
          .withMethod(verb)

        matchingAttempt
          .tryMatching(expectation, request)
          .httpMethodMatchResult shouldBe HttpMethodMatchResult(
          AnyHttpMethodMatcher,
          MatchingScore.success(AnyHttpMethodMatcher.maxScore)
        )
      }
    }

    "match specific" in {
      val verbs = Table(
        "verb",
        allVerbs: _*
      )

      forAll(verbs) { verb =>
        val httpMethodMatcher = HttpMethodMatcher.fromMethod(verb)
        val expectation = ServiceExpectation(httpMethodMatcher = httpMethodMatcher)

        val request = createRequest
          .withUri("http://www.x.com/xxxx/yyyy")
          .withMethod(verb)

        matchingAttempt
          .tryMatching(expectation, request)
          .httpMethodMatchResult shouldBe HttpMethodMatchResult(
          httpMethodMatcher,
          MatchingScore.success(httpMethodMatcher.maxScore)
        )

      }
    }

    "not match" in {
      val verbs = Table(
        "verb",
        allVerbs: _*
      )

      forAll(verbs) { verb =>
        val httpMethodMatcher = HttpMethodMatcher.fromMethod(verb)
        val expectation =
          ServiceExpectation(httpMethodMatcher = httpMethodMatcher)

        val request = createRequest
          .withUri("http://www.x.com/xxxx/yyyy")
          .withMethod(allVerbs.find(_ != verb).value)

        matchingAttempt
          .tryMatching(expectation, request)
          .httpMethodMatchResult shouldBe HttpMethodMatchResult(
          httpMethodMatcher,
          MatchingScore(total = 0, httpMethodMatcher.maxScore)
        )

      }
    }
  }

  "content matcher" should {
    "match against string equality" in {
      val content = "textcontent"
      val examples = Table("body", content, "none matching content")

      forAll(examples) { example =>
        val contentEquals = ContentEquals(example)
        val postWithContent = PostMatcher(contentEquals)

        val expectation =
          ServiceExpectation(httpMethodMatcher = postWithContent)

        val request = createRequest
          .withUri("http://www.x.com/xxxx/yyyy")
          .withMethod(Method.POST)
          .withBody(content)

        matchingAttempt
          .tryMatching(expectation, request)
          .contentMatchResult shouldBe ContentMatchResult(
          contentEquals,
          matchingScore = MatchingScore.fromMatch(content == example, contentEquals)
        )
      }
    }

    "match against regex matching" in {
      val content = "textcontent"
      val examples = Table(
        ("body", "success"),
        ("textco(\\w){5}", true),
        ("none matching content", false)
      )

      forAll(examples) {
        case (example, expectSuccess) =>
          val contentMatches = ContentMatches(example.r)
          val postWithContent = PostMatcher(contentMatches)

          val expectation =
            ServiceExpectation(httpMethodMatcher = postWithContent)

          val request = createRequest
            .withUri("http://www.x.com/xxxx/yyyy")
            .withMethod(Method.POST)
            .withBody(content)

          matchingAttempt
            .tryMatching(expectation, request)
            .contentMatchResult shouldBe ContentMatchResult(
            contentMatches,
            matchingScore = MatchingScore.fromMatch(expectSuccess, contentMatches)
          )
      }
    }

    "match against json ignoring formatting" in {
      val jsonContent =
        """
          | {
          |   "a": 1,
          |   "b" : 2
          | }
        """.stripMargin

      val examples = Table(
        ("body", "success"),
        ("""{ "a" : 1, "b": 2}""", true),
        ("{}", false)
      )

      forAll(examples) {
        case (example, expectSuccess) =>
          val jsonContentEquals = JsonContentEquals(example)
          val postWithContent = PostMatcher(jsonContentEquals)

          val expectation =
            ServiceExpectation(httpMethodMatcher = postWithContent)

          val request = createRequest
            .withUri("http://www.x.com/xxxx/yyyy")
            .withMethod(Method.POST)
            .withBody(jsonContent)

          matchingAttempt
            .tryMatching(expectation, request)
            .contentMatchResult shouldBe ContentMatchResult(
            jsonContentEquals,
            MatchingScore.fromMatch(expectSuccess, jsonContentEquals)
          )
      }
    }

    "not error if request has invalid json" in {
      val jsonContentEquals = JsonContentEquals("""{ "a" : 1, "b": 2}""")
      val postWithContent = PostMatcher(jsonContentEquals)

      val expectation =
        ServiceExpectation(httpMethodMatcher = postWithContent)

      val request = createRequest
        .withUri("http://www.x.com/xxxx/yyyy")
        .withMethod(Method.POST)
        .withBody("{:{")

      matchingAttempt
        .tryMatching(expectation, request)
        .contentMatchResult shouldBe ContentMatchResult(
        jsonContentEquals,
        MatchingScore.fromMatch(success = false, jsonContentEquals)
      )
    }

    "match invalid request json to invalid expectation json" in {
      val invalidJson = "{:{"
      val jsonContentEquals = JsonContentEquals(invalidJson)
      val postWithContent = PostMatcher(jsonContentEquals)

      val expectation =
        ServiceExpectation(httpMethodMatcher = postWithContent)

      val request = createRequest
        .withUri("http://www.x.com/xxxx/yyyy")
        .withMethod(Method.POST)
        .withBody("{:{")

      matchingAttempt
        .tryMatching(expectation, request)
        .contentMatchResult shouldBe ContentMatchResult(
        jsonContentEquals,
        MatchingScore.fromMatch(success = true, jsonContentEquals)
      )
    }

    "request is valid json but expectation is not" in {
      val invalidJson = "{:{"
      val jsonContentEquals = JsonContentEquals(invalidJson)
      val postWithContent = PostMatcher(jsonContentEquals)

      val expectation =
        ServiceExpectation(httpMethodMatcher = postWithContent)

      val request = createRequest
        .withUri("http://www.x.com/xxxx/yyyy")
        .withMethod(Method.POST)
        .withBody("{}")

      matchingAttempt
        .tryMatching(expectation, request)
        .contentMatchResult shouldBe ContentMatchResult(
        jsonContentEquals,
        MatchingScore.fromMatch(success = false, jsonContentEquals)
      )
    }

  }

  "header matcher is case insensitive on name and" should {
    val headers =
      List(
        Header("X", "1"),
        Header("y", "2"),
        Header("a", "y"),
        Header("z", "zoo")
      )

    val requestWithHeaders =
      createRequest.withHeaders(headers)

    "not care about order and allow a combination of matching and equality" in {
      val headerEqualsMatchers = Vector(
        ("y", "2"),
        ("x", "1")
      ).map(_.asHeaderEquals)

      val headerMatchesMatchers = Vector(
        ("z", "z(o){2}"),
        ("a", "(.)")
      ).map(_.asHeaderMatches)

      val successMatchScore = MatchingScore(1, 1)

      val headerMatches = Vector(
        HeaderMatchResult(headerEqualsMatchers(0), successMatchScore),
        HeaderMatchResult(headerEqualsMatchers(1), successMatchScore),
        HeaderMatchResult(headerMatchesMatchers(0), successMatchScore),
        HeaderMatchResult(headerMatchesMatchers(1), successMatchScore)
      )

      val expectation =
        ServiceExpectation(headerMatchers = headerEqualsMatchers ++ headerMatchesMatchers)

      matchingAttempt
        .tryMatching(
          expectation,
          requestWithHeaders
        )
        .headerMatchResults shouldBe headerMatches
    }

    "not care about order and allow a combination of matching and equality for headers failing on either" in {
      val headerEqualsMatchers = Vector(
        ("y", "3"),
        ("x", "2")
      ).map(_.asHeaderEquals)

      val headerMatchesMatchers = Vector(
        ("z", "m(o){3}"),
        ("a", "(.){2}")
      ).map(_.asHeaderMatches)

      val failMatchScore = MatchingScore(0, 1)

      val paramMatches = Vector(
        HeaderMatchResult(headerEqualsMatchers(0), failMatchScore),
        HeaderMatchResult(headerEqualsMatchers(1), failMatchScore),
        HeaderMatchResult(headerMatchesMatchers(0), failMatchScore),
        HeaderMatchResult(headerMatchesMatchers(1), failMatchScore)
      )

      val expectation =
        ServiceExpectation(headerMatchers = headerEqualsMatchers ++ headerMatchesMatchers)

      matchingAttempt
        .tryMatching(
          expectation,
          requestWithHeaders
        )
        .headerMatchResults shouldBe paramMatches
    }
  }
}
