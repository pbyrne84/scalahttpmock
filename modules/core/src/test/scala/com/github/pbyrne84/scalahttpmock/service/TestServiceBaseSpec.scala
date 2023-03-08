package com.github.pbyrne84.scalahttpmock.service

import com.github.pbyrne84.scalahttpmock.shared.BaseSpec

abstract class TestServiceBaseSpec extends BaseSpec {

  protected val expectation: TestServiceExpectations = new TestServiceExpectations

}
