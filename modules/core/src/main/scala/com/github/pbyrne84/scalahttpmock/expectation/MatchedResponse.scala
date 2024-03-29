package com.github.pbyrne84.scalahttpmock.expectation

class InvalidResponseStatusCodeException(message: String) extends IllegalArgumentException(message)

object Header {
  def apply(name: String, value: String): Header = {
    Header(CaseInsensitiveString(name), value)
  }
  def location(url: String): Header = Header(CaseInsensitiveString("Location"), url)
  def contentType(contentType: String): Header =
    Header(CaseInsensitiveString("Content-Type"), contentType)
}

case class Header(name: CaseInsensitiveString, value: String)

case class Status(code: Int)

object Status {
  def fromInt(status: Int): Status = Status(status)
}

object MatchedResponse {
  // collated from the children of org.http4s.dsl.impl.ResponseGenerator
  private[expectation] val entityResponseCodes: Seq[Int] = Vector(
    200, 201, 202, 203, 206, 207, 208, 226, 400, 402, 403, 404, 405, 406, 407, 408, 409, 410, 411, 412, 413, 414, 415,
    416, 417, 422, 423, 424, 426, 428, 429, 431, 451, 500, 501, 502, 503, 504, 505, 506, 507, 508, 510, 511
  )

  private[expectation] val emptyResponseCodes: Seq[Int] = Vector(100, 101, 204, 205, 304)

  private[expectation] val locationResponseCodes: Seq[Int] = Vector(300, 301, 302, 303, 307, 308)
}

sealed trait MatchedResponse {

  val statusCode: Int
  def customHeaders: Seq[Header]

  private[scalahttpmock] val status = Status
    .fromInt(statusCode)

  def allHeaders: Seq[Header]
}

trait MatchedResponseWithPotentialBody extends MatchedResponse {
  val contentTypeHeader: Header

  val maybeBody: Option[String]

  def allHeaders: Seq[Header] =
    Vector(Header(contentTypeHeader.name, contentTypeHeader.value)) ++ customHeaders

  if (!MatchedResponse.entityResponseCodes.contains(statusCode)) {
    throw new InvalidResponseStatusCodeException(
      s"Status code $statusCode does not support returning a body"
    )
  }

}

trait JsonContentType { self: MatchedResponseWithPotentialBody =>
  override val contentTypeHeader: Header = {
    Header.contentType("application/json")
  }

}

case class JsonResponse(statusCode: Int, maybeBody: Option[String] = None, customHeaders: Seq[Header] = Seq())
    extends MatchedResponseWithPotentialBody
    with JsonContentType

case class EmptyResponse(statusCode: Int, customHeaders: Seq[Header] = Seq()) extends MatchedResponse {

  override val allHeaders: Seq[Header] = customHeaders

  if (!MatchedResponse.emptyResponseCodes.contains(statusCode)) {
    throw new InvalidResponseStatusCodeException(
      s"Status code $statusCode is not an empty response"
    )
  }
}

object LocationResponse {
  def apply(statusCode: Int, uri: String, customHeaders: Seq[Header] = Seq()) =
    new LocationResponse(statusCode, uri, customHeaders)
}

case class LocationResponse private[expectation] (statusCode: Int, uri: String, customHeaders: Seq[Header])
    extends MatchedResponse {
  if (!MatchedResponse.locationResponseCodes.contains(statusCode)) {
    throw new InvalidResponseStatusCodeException(
      s"Status code $statusCode does not support location changes"
    )
  }

  private val locationHeader = Header.location(uri)

  override val allHeaders: Seq[Header] = locationHeader +: customHeaders
}
