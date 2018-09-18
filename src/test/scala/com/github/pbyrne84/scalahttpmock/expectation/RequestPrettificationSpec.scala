package com.github.pbyrne84.scalahttpmock.expectation

import com.github.pbyrne84.scalahttpmock.BaseSpec
import org.http4s.{Header, Headers}

class RequestPrettificationSpec extends BaseSpec {

  import RequestPrettification._

  "RequestPrettification" should {

    "format a default entry" in {
      createRequest.prettyFormat shouldBe
        """
          |Request[method="GET", path="/"](
          |  Uri     : "/",
          |  Params  : [],
          |  Headers : [],
          |  Body    : None
          |)
          |""".stripMargin.trim
    }

    "format an entry with a uri that has params, params will be displayed alphabetically" in {
      val uriText = "http://xxx.com/a/b/c?f=1&d=2&e=&e=10"
      val actual = createRequest
        .withUri(uriText.asUri)
        .prettyFormat

      actual shouldBe
        s"""
          |Request[method="GET", path="/a/b/c"](
          |  Uri     : "$uriText",
          |  Params  : [ ("d", "2"),
          |              ("e", ""),
          |              ("e", "10"),
          |              ("f", "1") ],
          |  Headers : [],
          |  Body    : None
          |)
          |""".stripMargin.trim

    }

    "format an entry with headers sorted by name" in {
      val headers = Headers(
        List(
          Header("f", "1"),
          Header("d", "2"),
          Header("e", ""),
          Header("e", "10")
        )
      )

      val actual = createRequest.withHeaders(headers).prettyFormat
      actual shouldBe
        s"""
          |Request[method="GET", path="/"](
          |  Uri     : "/",
          |  Params  : [],
          |  Headers : [ ("d", "2"),
          |              ("e", ""),
          |              ("e", "10"),
          |              ("f", "1") ],
          |  Body    : None
          |)
          |""".stripMargin.trim

    }

    "format an entry with a body" in {
      val actual = createRequest
        .withBody("content")
        .unsafeRunSync()
        .prettyFormat

      actual shouldBe
        s"""
          |Request[method="GET", path="/"](
          |  Uri     : "/",
          |  Params  : [],
          |  Headers : [ ("Content-Length", "7"),
          |              ("Content-Type", "text/plain; charset=UTF-8") ],
          |  Body    : content
          |)
          |""".stripMargin.trim

    }

  }

}
