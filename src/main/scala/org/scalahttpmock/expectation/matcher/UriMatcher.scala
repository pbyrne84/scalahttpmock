package org.scalahttpmock.expectation.matcher

import org.scalahttpmock.expectation.{HasAnyMatchMaxScore, HasMaxScore}

import scala.util.matching.Regex

sealed trait UriMatcher extends HasMaxScore with PrettyText {
  override def maxScore: Double = 1
}
case object AnyUriMatcher extends UriMatcher with HasAnyMatchMaxScore {
  override val prettyText: String = "Any"
}

case class UriEquals(uri: String) extends UriMatcher {
  override val prettyText: String = s"""Uri equals "$uri""""
}

case class UriMatches(uriRegex: Regex) extends UriMatcher {
  override val prettyText: String = s"""Uri matches "$uriRegex""""
}

case class PathEquals(path: String) extends UriMatcher {
  override val prettyText: String = s"""Path equals "$path""""
}

case class PathMatches(pathRegex: Regex) extends UriMatcher {
  override val prettyText: String = s"""Path matches "$pathRegex""""
}
