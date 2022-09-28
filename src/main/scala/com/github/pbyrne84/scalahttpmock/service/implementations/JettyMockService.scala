package com.github.pbyrne84.scalahttpmock.service.implementations

import com.github.pbyrne84.scalahttpmock.expectation.{
  MatchableRequest,
  MatchingAttempt,
  ServiceExpectation
}
import com.github.pbyrne84.scalahttpmock.service.executor.{
  FutureMockServiceExecutor,
  MockServiceExecutor,
  RunningServer
}
import com.github.pbyrne84.scalahttpmock.service.request.{PotentialResponse, RequestMatching}
import com.typesafe.scalalogging.LazyLogging
import org.eclipse.jetty.server.Server

import scala.concurrent.{ExecutionContext, Future}

object JettyMockService {

  def createFutureVersion(port: Int)(implicit ec: ExecutionContext): JettyMockService[Future] = {
    val futureMockServiceExecutor = new FutureMockServiceExecutor
    new JettyMockService(port, futureMockServiceExecutor)
  }
}

class JettyMockService[F[_]](port: Int, mockServiceExecutor: MockServiceExecutor[F])
    extends LazyLogging {
  private[this] val requestMatching = new RequestMatching(new MatchingAttempt)
  private val server = new Server(port)

  def start(): F[RunningServer[F]] = {
    val server = new Server(port)
    server.setHandler(mockHandler)

    mockServiceExecutor.run(server)
  }

  def reset(): Unit = requestMatching.reset()

  private[implementations] def resolveResponse(
      matchableRequest: MatchableRequest
  ): PotentialResponse =
    requestMatching
      .resolveResponse(matchableRequest)

  def addExpectation(expectation: ServiceExpectation): JettyMockService[F] = {
    requestMatching.addExpectation(expectation)
    this
  }

  def verifyCall(expectation: ServiceExpectation, expectedTimes: Int = 1): Unit =
    requestMatching.verifyCall(expectation, expectedTimes)

  private lazy val mockHandler = new JettyMockServiceHandler(server, this)
}
