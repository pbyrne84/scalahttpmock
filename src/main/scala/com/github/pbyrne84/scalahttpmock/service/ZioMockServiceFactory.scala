package com.github.pbyrne84.scalahttpmock.service

import com.github.pbyrne84.scalahttpmock.expectation.{MatchingAttempt, ServiceExpectation}
import com.github.pbyrne84.scalahttpmock.service.request.RequestMatching
import com.typesafe.scalalogging.LazyLogging
import org.log4s.getLogger
import zhttp.http._
import zhttp.service.Server
import zio._

class ZioMockService private[service] (port: Int) extends App with LazyLogging {
  private[this] val requestMatching = new RequestMatching(new MatchingAttempt)
  private[this] val messageFailureLogger = getLogger("org.http4s.server.message-failures")
  private[this] val serviceErrorLogger = getLogger("org.http4s.server.service-errors")

  val app: Http[Any, Nothing, Request, UResponse] = Http.collect[Request] {
    case Method.GET -> Root / "text" => Response.text("Hello World!")
  }

  val matchingCall: PartialFunction[Request, UResponse] = new PartialFunction[Request, UResponse] {
    def apply(x: Request) = Response.text("Hello World!")

    def isDefinedAt(x: Request) = true
  }

  val app2: Http[Any, Nothing, Request, UResponse] = Http.collect[Request].apply {
    matchingCall
  }

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    Server.start(8090, app2).exitCode

  //  private val mockINGHttpService = HttpRoutes
  //    .of[IO] {
  //      case request: Request[IO] =>
  //        val potentialResponse = requestMatching
  //          .resolveResponse(request)
  //
  //        potentialResponse.maybeResponse
  //          .map(response => ResponseRemapping.respondSuccessfully(response))
  //          .getOrElse(
  //            ResponseRemapping.respondUnSuccessfully(request, potentialResponse.allAttempts)
  //          )
  //
  //      case unknown =>
  //        messageFailureLogger.info(s"failed matching request\n$unknown")
  //        IO(Response(Status.NotImplemented))
  //    }
  //    .orNotFound

  def reset(): Unit = requestMatching.reset()

  def addExpectation(expectation: ServiceExpectation): Unit =
    requestMatching.addExpectation(expectation)

  def verifyCall(expectation: ServiceExpectation, expectedTimes: Int = 1): Unit =
    requestMatching.verifyCall(expectation, expectedTimes)

  def shutDown(): Unit = {} //server.shutdownNow()
}

object MockServiceFactory {

  def create(port: Int): MockService = new MockService(port)
}
