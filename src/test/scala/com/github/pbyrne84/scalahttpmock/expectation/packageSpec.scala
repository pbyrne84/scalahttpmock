package com.github.pbyrne84.scalahttpmock.expectation

import com.github.pbyrne84.scalahttpmock.BaseSpec
import org.eclipse.jetty.server.Request
import org.http4s.Uri.{Authority, Scheme}
import org.http4s.{Query, Uri}

class packageSpec extends BaseSpec {

  "UriAs asPathWithParams" should {
    val authority = Authority(None, port = Some(8080))

    "convert when there are no params" in {
      Uri(Some(Scheme.http), path = "/", authority = Some(authority)).asPathWithParams shouldBe "/"
    }

    "convert when there are params" in {
      Uri(
        Some(Scheme.http),
        path = "/",
        authority = Some(authority),
        query = Query("a" -> Some("aa"), "a" -> Some("aaa"), "b" -> Some("bb"))
      ).asPathWithParams shouldBe "/?a=aa&a=aaa&b=bb"
    }
  }

  "RequestOps asPathWithParams" should {

    "convert when there are no params" in {
      val request = createServletRequest("/a/v/s/c", None)

      request.asPathWithParams shouldBe "/a/v/s/c"
    }

    "convert when there are params" in {
      val request = createServletRequest("/a/v/s/c", Some("a=1"))

      request.asPathWithParams shouldBe "/a/v/s/c?a=1"
    }

    def createServletRequest(pathInfo: String, maybeParams: Option[String]): Request = {
      //trying to work out the innards of this is lovely
      val request = mock[Request]

      (request.getPathInfo _)
        .expects()
        .returns(pathInfo)

      (request.getQueryString _).expects().returns(maybeParams.orNull)

      request
    }
  }
}
