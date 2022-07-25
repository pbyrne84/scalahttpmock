package com.github.pbyrne84.scalahttpmock.expectation

import com.github.pbyrne84.scalahttpmock.BaseSpec
import org.eclipse.jetty.server.Request

class packageSpec extends BaseSpec {

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

      import org.mockito.Mockito._

      val request = mock(classOf[Request])

      when(request.getPathInfo)
        .thenReturn(pathInfo)

      when(request.getQueryString)
        .thenReturn(maybeParams.orNull)

      request
    }
  }
}
