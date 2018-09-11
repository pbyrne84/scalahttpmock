package com.github.pbyrne84.scalahttpmock.expectation.matcher
import com.github.pbyrne84.scalahttpmock.expectation.HasMaxScore

import scala.util.matching.Regex

trait PrettyText {
  val prettyText: String
}

sealed trait HeaderMatcher extends HasMaxScore with PrettyText

trait MatchPrettyText extends PrettyText {
  val name: String
  val valueRegex: Regex

  val prettyText: String = s"""Match("$name", "$valueRegex")""".stripMargin
}

trait EqualsPrettyText extends PrettyText {
  val name: String
  val value: String

  val prettyText: String = s"""Equals("$name", "$value")""".stripMargin
}

case class HeaderMatches(name: String, valueRegex: Regex)
    extends HeaderMatcher
    with MatchPrettyText {
  override def maxScore: Double = 1

}

case class HeaderEquals(name: String, value: String) extends HeaderMatcher with EqualsPrettyText {
  override def maxScore: Double = 1
}
