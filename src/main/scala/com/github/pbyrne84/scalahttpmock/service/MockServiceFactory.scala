package com.github.pbyrne84.scalahttpmock.service

import cats.Monad
import cats.effect.{ExitCode, IO, IOApp}
import com.github.pbyrne84.scalahttpmock.expectation.{MatchingAttempt, ServiceExpectation}
import com.github.pbyrne84.scalahttpmock.service.request.RequestMatching
import com.github.pbyrne84.scalahttpmock.service.response.ResponseRemapping
import com.typesafe.scalalogging.LazyLogging
import org.http4s._
import org.http4s.headers.{`Content-Length`, Connection}
import org.http4s.implicits._
import org.http4s.server.ServiceErrorHandler
import org.log4s.getLogger

import scala.language.higherKinds

class MockService private[service] (port: Int) extends IOApp with LazyLogging {
  private[this] val requestMatching = new RequestMatching(new MatchingAttempt)
  private[this] val messageFailureLogger = getLogger("org.http4s.server.message-failures")
  private[this] val serviceErrorLogger = getLogger("org.http4s.server.service-errors")

  override def run(args: List[String]): IO[ExitCode] =
    throw new NotImplementedError("not needed at the moment and io app has the implicits needed")

  private val mockINGHttpService = HttpRoutes
    .of[IO] {
      case request: Request[IO] =>
        val potentialResponse = requestMatching
          .resolveResponse(request)

        potentialResponse.maybeResponse
          .map(response => ResponseRemapping.respondSuccessfully(response))
          .getOrElse(
            ResponseRemapping.respondUnSuccessfully(request, potentialResponse.allAttempts)
          )

      case unknown =>
        messageFailureLogger.info(s"failed matching request\n$unknown")
        IO(Response(Status.NotImplemented))
    }
    .orNotFound

  private def DefaultServiceErrorHandler[F[_]](implicit F: Monad[F]): ServiceErrorHandler[F] =
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
                   Headers.of(
                     Connection("close".ci),
                     `Content-Length`.zero
                   ))
        )

    }

  import cats.effect._
  import org.http4s.server.blaze._

  private val builder = BlazeServerBuilder[IO]
    .bindHttp(port, "localhost")
    .withHttpApp(mockINGHttpService)
    .withServiceErrorHandler(DefaultServiceErrorHandler)

  private val server = builder.resource.use(_ => IO.never).start.unsafeRunSync()

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
