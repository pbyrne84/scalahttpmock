package com.github.pbyrne84.scalahttpmock.service.executor

import com.github.pbyrne84.scalahttpmock.expectation.ServiceExpectation
import com.github.pbyrne84.scalahttpmock.service.request.RequestMatching

class RunningMockServerWithOperations[F[_]](
    runningServer: RunningServer[F],
    requestMatching: RequestMatching
) {

  // Don't necessarily need to call this as things can be shared between tests
  def shutDown: F[Either[Throwable, Unit]] = runningServer.shutDown

  def addExpectation(serviceExpectation: ServiceExpectation): F[Unit] = {
    runningServer.wrapInTask(requestMatching.addExpectation(serviceExpectation))
  }

  def reset: F[Unit] =
    runningServer.wrapInTask(requestMatching.reset())

  def verifyCall(expectation: ServiceExpectation, expectedTimes: Int = 1): F[Unit] =
    runningServer.wrapInTask(requestMatching.verifyCall(expectation, expectedTimes))

}
