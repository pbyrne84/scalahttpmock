package org.scalahttpmock.expectation.matcher
import org.scalahttpmock.expectation.HasMaxScore

import scala.util.matching.Regex

sealed trait ParamMatcher extends HasMaxScore with PrettyText {
  override def maxScore: Double = 1
}
case class ParamMatches(name: String, valueRegex: Regex) extends ParamMatcher with MatchPrettyText
case class ParamEquals(name: String, value: String) extends ParamMatcher with EqualsPrettyText
