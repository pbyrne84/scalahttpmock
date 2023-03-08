package com.github.pbyrne84.scalahttpmock.testextensions

import com.github.pbyrne84.scalahttpmock.expectation.matcher._
import com.github.pbyrne84.scalahttpmock.service.URI

object TestStringOps {

  implicit class StringAsMatcher(string: String) {
    def asUri: URI = URI(string)

    def asPathEquals: UriMatcher = PathEquals(string)

    def asPathMatches: UriMatcher = PathMatches(string.r)

    def asUriEquals: UriMatcher = UriEquals(string)

    def asUriMatches: UriMatcher = UriMatches(string.r)

    def asJsonPostMatcher: PostMatcher = PostMatcher(JsonContentEquals(string))
  }

}
