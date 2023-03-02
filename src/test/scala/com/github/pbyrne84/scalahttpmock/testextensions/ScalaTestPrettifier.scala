package com.github.pbyrne84.scalahttpmock.testextensions

import com.github.pbyrne84.scalahttpmock.prettifier.CaseClassPrettifier
import org.scalactic.Prettifier

object ScalaTestPrettifier {

  val prettifier: Prettifier = Prettifier.apply {
    case a: AnyRef if CaseClassPrettifier.shouldBeUsedInTestMatching(a) =>
      new CaseClassPrettifier().prettify(a)

    case a: Any =>
      Prettifier.default(a)

  }

}
