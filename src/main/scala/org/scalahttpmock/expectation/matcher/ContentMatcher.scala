package org.scalahttpmock.expectation.matcher
import io.circe.Json
import io.circe.parser._
import org.scalahttpmock.expectation.{HasAnyMatchMaxScore, HasMaxScore}

import scala.util.matching.Regex

sealed trait ContentMatcher extends HasMaxScore with PrettyText

case object AnyContentMatcher extends ContentMatcher with HasAnyMatchMaxScore {
  override val prettyText: String = "Any"
}
case class ContentEquals(content: String) extends ContentMatcher {
  override def maxScore: Double = 1
  override val prettyText: String = s"""Equals "$content" """
}
case class ContentMatches(contentRegex: Regex) extends ContentMatcher {
  override def maxScore: Double = 1
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
  override val prettyText: String = eitherInvalidJsonOrParsedJson match {
    case Left(invalidJson) => invalidJson
    case Right(parsedJson) => parsedJson.spaces2
  }
}
