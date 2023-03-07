package com.github.pbyrne84.scalahttpmock.testextensions

import com.github.pbyrne84.scalahttpmock.expectation.{Header, MatchableRequest, MockHttpMethod}
import com.github.pbyrne84.scalahttpmock.service.URI

object MatchableRequestOps {

  implicit class MatchableRequestOps(matchableRequest: MatchableRequest) {
    def withUri(uriString: String): MatchableRequest = {
      val uri = URI(uriString)

      val uriPath = uri.path

      matchableRequest.copy(
        uriPath = uriPath,
        uri = uri.uri,
        asPathWithParams = uri.pathWithParams,
        multiParams = uri.params
      )

    }

    def withMethod(method: MockHttpMethod): MatchableRequest = {
      matchableRequest.copy(method = method)
    }

    def withBody(body: String): MatchableRequest = {
      val headers = List(
        Header("Content-Length", body.length.toString),
        Header("Content-Type", "text/plain; charset=UTF-8")
      )
      matchableRequest.copy(
        maybeContentAsString = Some(body),
        headers = headers
      )
    }

    def withHeaders(headers: List[Header]): MatchableRequest = {
      matchableRequest.copy(headers = headers)
    }
  }
}
