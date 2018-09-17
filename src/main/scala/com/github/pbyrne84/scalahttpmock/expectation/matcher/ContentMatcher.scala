package com.github.pbyrne84.scalahttpmock.expectation.matcher
import com.github.pbyrne84.scalahttpmock.expectation.{HasAnyMatchMaxScore, HasMaxScore}
import io.circe.Json
import io.circe.parser._

import scala.util.matching.Regex

sealed trait ContentMatcher extends HasMaxScore with PrettyText {
  val shortDescription: String
}

case object AnyContentMatcher extends ContentMatcher with HasAnyMatchMaxScore {
  override val shortDescription: String = "Any"
  override val prettyText: String = shortDescription
}
case class ContentEquals(content: String) extends ContentMatcher {
  override def maxScore: Double = 1

  override val shortDescription: String = "Equals text"
  override val prettyText: String = s"""Equals "$content" """
}
case class ContentMatches(contentRegex: Regex) extends ContentMatcher {
  override def maxScore: Double = 1

  override val shortDescription: String = "Matches regex"
  override val prettyText: String = s"""Matches "$contentRegex" """
}

object JsonContentEquals {
  def apply(json: String): JsonContentEquals = {
    parse(json) match {
      case Left(e) => new JsonContentEquals(Left(json))
      case Right(parsedJson) => new JsonContentEquals(Right(parsedJson))
    }
  }
}

case class JsonContentEquals private[matcher] (eitherInvalidJsonOrParsedJson: Either[String, Json])
    extends ContentMatcher {
  override def maxScore: Double = 1

  override val shortDescription: String = "Equals Json"

  override val prettyText: String = eitherInvalidJsonOrParsedJson match {
    case Left(invalidJson) => invalidJson
    case Right(parsedJson) => parsedJson.spaces2
  }
}
