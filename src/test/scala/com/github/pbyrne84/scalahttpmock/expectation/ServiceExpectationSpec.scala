package com.github.pbyrne84.scalahttpmock.expectation
import com.github.pbyrne84.scalahttpmock.BaseTest
import com.github.pbyrne84.scalahttpmock.expectation.matcher.{JsonContentEquals, PostMatcher}

class ServiceExpectationSpec extends BaseTest {

  "prettyFormat" should {
    "display expectation in human readable form for the default which is any anything" in {
      val actual = ServiceExpectation().prettyFormat
      val expected = """
          |ServiceExpectation[method="Any"](
          |  Headers : Any,
          |  Uri     : Any,
          |  Params  : Any,
          |  Body    : Any
          |)""".stripMargin.trim

      actual shouldBe expected
    }

    "display expectation in human readable form for everything when there is one for those that accept multiple" in {
      val actual = ServiceExpectation(
        Vector(("header_name", "header_value").asHeaderMatches),
        PostMatcher(JsonContentEquals("""{"a":"1"}""")),
        "/a/b/c".asPathEquals,
        Vector(("param_name", "param_value").asParamEquals)
      ).prettyFormat

      val expected = """
                       |ServiceExpectation[method="Post"](
                       |  Headers : [ Match("header_name", "header_value") ],
                       |  Uri     : Path equals "/a/b/c",
                       |  Params  : [ Equals("param_name", "param_value") ],
                       |  Body    : {
                       |    "a" : "1"
                       |  }
                       |)""".stripMargin.trim

      actual shouldBe expected
    }

    "format multiple header types in a readable format" in {
      val actual = ServiceExpectation(
        Vector(
          ("header_name1", "header_value1").asHeaderMatches,
          ("header_name2", "header_value2").asHeaderEquals,
          ("header_name3", "header_value3").asHeaderMatches
        )
      ).prettyFormat

      val expected = """
                       |ServiceExpectation[method="Any"](
                       |  Headers : [ Match("header_name1", "header_value1"),
                       |              Equals("header_name2", "header_value2"),
                       |              Match("header_name3", "header_value3") ],
                       |  Uri     : Any,
                       |  Params  : Any,
                       |  Body    : Any
                       |)""".stripMargin.trim

      actual shouldBe expected

    }
  }

  "format multiple parameter types in a readable format" in {
    val actual = ServiceExpectation(
      paramMatchers = Vector(
        ("param_name1", "param_value1").asParamMatches,
        ("param_name2", "param_value2").asParamEquals,
        ("param_name3", "param_value3").asParamMatches
      )
    ).prettyFormat

    val expected = """
                       |ServiceExpectation[method="Any"](
                       |  Headers : Any,
                       |  Uri     : Any,
                       |  Params  : [ Match("param_name1", "param_value1"),
                       |              Equals("param_name2", "param_value2"),
                       |              Match("param_name3", "param_value3") ],
                       |  Body    : Any
                       |)""".stripMargin.trim

    actual shouldBe expected

  }

}
