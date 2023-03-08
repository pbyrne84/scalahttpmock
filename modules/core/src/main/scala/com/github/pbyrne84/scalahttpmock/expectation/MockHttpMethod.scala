package com.github.pbyrne84.scalahttpmock.expectation

object MockHttpMethod {
  private val options: Map[String, MockHttpMethod] = List(POST, GET, PUT, DELETE, PATCH).map { value =>
    value.text -> value
  }.toMap
  def fromString(text: String): Option[MockHttpMethod] = {
    options.get(text.toUpperCase)
  }

  case object POST extends MockHttpMethod("POST")

  case object GET extends MockHttpMethod("GET")

  case object PUT extends MockHttpMethod("PUT")

  case object DELETE extends MockHttpMethod("DELETE")

  case object PATCH extends MockHttpMethod("PATCH")

}

sealed abstract class MockHttpMethod(val text: String)
