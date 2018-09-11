package com.github.pbyrne84.scalahttpmock.expectation

import org.http4s.headers._
import org.http4s.{Header, MediaType}

import scala.collection.immutable.Seq

sealed trait MatchedResponse {
  val typeHeader: `Content-Type`
  val statusCode: Int
  val customHeaders: Seq[Header]
  val maybeBody: Option[String]

  lazy val allHeaders: Seq[Header] = Vector(
    Header(typeHeader.name.value, typeHeader.value)
  ) ++ customHeaders
}

trait JsonContentType extends MatchedResponse {
  val typeHeader: `Content-Type` = `Content-Type`(MediaType.`application/json`)
}

case class JsonResponse(statusCode: Int,
                        maybeBody: Option[String] = None,
                        customHeaders: Seq[Header] = Seq())
    extends MatchedResponse
    with JsonContentType
