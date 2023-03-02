package com.github.pbyrne84.scalahttpmock.service

import com.github.pbyrne84.scalahttpmock.BaseSpec

class URISpec extends BaseSpec {

  "apply" should {
    "create a URI where there is a basic path and it is also on the uri string" in {
      val uriText = "http://test.com:8080/"
      URI(uriText) shouldBe URI(uri = uriText, path = "/", pathWithParams = "/", maybeQuery = None, Map.empty)
    }

    "create a URI where there is a basic path and it is not on the uri string" in {
      val uriText = "http://test.com"
      URI(uriText) shouldBe URI(uri = uriText, path = "/", pathWithParams = "/", maybeQuery = None, params = Map.empty)
    }

    "create a URI that is more complicated with params" in {
      val uriText = "http://test.com/a/b/c?aa=1&bb=2&aa=2"
      URI(uriText) shouldBe URI(
        uri = uriText,
        path = "/a/b/c",
        pathWithParams = "/a/b/c?aa=1&bb=2&aa=2",
        maybeQuery = Some("aa=1&bb=2&aa=2"),
        params = Map("aa" -> List("1", "2"), "bb" -> List("2"))
      )
    }

  }
}
