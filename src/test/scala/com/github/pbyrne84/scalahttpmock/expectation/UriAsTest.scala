package com.github.pbyrne84.scalahttpmock.expectation
import com.github.pbyrne84.scalahttpmock.BaseTest

class UriAsTest extends BaseTest {

  "uri as" should {

    "convert a uri with no port, no path and no params to a simple forward slash" in {
      "http://www.xxx.com/".asUri.asPathWithParams shouldBe "/"
    }

    "convert a uri with no port, a path and no params to path only" in {
      "http://www.xxx.com/aaa/bbb".asUri.asPathWithParams shouldBe "/aaa/bbb"
    }

    "convert a uri a port, a path and no params to a path with no params" in {
      "http://www.xxx.com:8080/aaa/bbb".asUri.asPathWithParams shouldBe "/aaa/bbb"
    }

    "convert a uri a port, a path and also with params to a path with params" in {
      "http://www.xxx.com:8080/aaa/bbb?a=1&b=2&b=3".asUri.asPathWithParams shouldBe "/aaa/bbb?a=1&b=2&b=3"
    }

  }

}
