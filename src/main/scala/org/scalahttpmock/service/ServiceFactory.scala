package org.scalahttpmock.service

import cats.Monad
import cats.effect.IO
import ch.qos.logback.classic.Logger
import com.typesafe.scalalogging.LazyLogging
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.headers.{`Content-Length`, Connection}
import org.http4s.server.ServiceErrorHandler
import org.http4s.server.blaze.BlazeBuilder
import org.log4s.getLogger
import org.scalahttpmock.expectation.{MatchingAttempt, ServiceExpectation}
import org.scalahttpmock.service.request.RequestMatching
import org.scalatest.Matchers

class TestService(port: Int) extends Matchers with LazyLogging {
  private[this] val requestMatching = new RequestMatching(new MatchingAttempt)

  private[this] val messageFailureLogger = getLogger("org.http4s.server.message-failures")
  private[this] val serviceErrorLogger = getLogger("org.http4s.server.service-errors")

  private val mockINGHttpService = HttpService[IO] {
    case request: Request[IO] =>
      requestMatching
        .resolveResponse(request)
        .maybeResponse
        .map(response => ResponseRemapping.respond(response))
        .getOrElse(IO(Response[IO](Status.NotImplemented)))

    case unknown =>
      messageFailureLogger.info(s"failed matching request\n$unknown")
      IO(Response(Status.NotImplemented))
  }

  private def DefaultServiceErrorHandler2[F[_]](implicit F: Monad[F]): ServiceErrorHandler[F] =
    req => {
      case mf: MessageFailure =>
        messageFailureLogger.debug(mf)(
          s"""Message failure handling request: ${req.method} ${req.pathInfo} from ${req.remoteAddr
            .getOrElse("<unknown>")}"""
        )
        mf.toHttpResponse(req.httpVersion)
      case t if !t.isInstanceOf[VirtualMachineError] =>
        serviceErrorLogger.error(t)(
          s"""Error servicing request: ${req.method} ${req.pathInfo} from ${req.remoteAddr
            .getOrElse(
              "<unknown>"
            )}"""
        )

        F.pure(
          Response(Status.NotFound,
                   req.httpVersion,
                   Headers(
                     Connection("close".ci),
                     `Content-Length`.zero
                   ))
        )

    }

  val root: Logger = org.slf4j.LoggerFactory
    .getLogger("ROOT")
    .asInstanceOf[Logger]

  root.setLevel(ch.qos.logback.classic.Level.ERROR)

  private val builder = BlazeBuilder[IO]
    .bindHttp(port, "localhost")
    .mountService(mockINGHttpService, "/")
    .withServiceErrorHandler(DefaultServiceErrorHandler2)
    .start
  private val server = builder.unsafeRunSync()

  def reset(): Unit = requestMatching.reset()

  def addExpectation(expectation: ServiceExpectation): Unit =
    requestMatching.addExpectation(expectation)

  def verifyCall(expectation: ServiceExpectation): Unit =
    requestMatching.verifyCall(expectation)

  def shutDown(): Unit = server.shutdownNow()
}

object ServiceFactory {

  def create(port: Int): TestService = new TestService(port)
}
