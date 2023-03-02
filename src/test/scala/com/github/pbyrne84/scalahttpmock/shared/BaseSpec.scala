package com.github.pbyrne84.scalahttpmock.shared

import com.github.pbyrne84.scalahttpmock.testextensions.{ScalaTestPrettifier, TestMatchFactory}
import org.scalactic.Prettifier
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import scala.language.implicitConversions

abstract class BaseSpec extends AnyWordSpec with Matchers {

  implicit val prettifier: Prettifier = ScalaTestPrettifier.prettifier
  protected val testMatchFactory = new TestMatchFactory

}
