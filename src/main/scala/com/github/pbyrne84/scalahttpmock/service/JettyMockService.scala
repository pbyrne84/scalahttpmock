package com.github.pbyrne84.scalahttpmock.service

import com.github.pbyrne84.scalahttpmock.expectation.{
  EmptyResponse,
  LocationResponse,
  MatchableRequest,
  MatchedResponse,
  MatchedResponseWithPotentialBody,
  MatchingAttempt,
  ServiceExpectation
}
import com.github.pbyrne84.scalahttpmock.service.request.{RequestMatching, UnSuccessfulResponse}
import com.typesafe.scalalogging.LazyLogging
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.eclipse.jetty.server.{Handler, Server}

import java.util.EventListener
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.Try

object JettyMockService {
  import scala.concurrent.ExecutionContext.Implicits.global
  def createFutureVersion(port: Int): JettyMockService[Future] = {
    implicit val a = new FutureMockServiceExecutor
    new JettyMockService(port)
  }
}

trait MockServiceExecutor[F[_]] {

  def run(server: Server): F[Server]

  def shutDown(runningServer: F[Server]): Either[Throwable, Unit]
}

class FutureMockServiceExecutor(implicit val ec: ExecutionContext)
    extends MockServiceExecutor[Future] {

  override def run(server: Server): Future[Server] = {
    Future {
      server.start()
      server.join()
      server
    }
  }

  override def shutDown(runningServer: Future[Server]): Either[Throwable, Unit] = {
    runningServer.foreach { server =>
      server.setStopAtShutdown(true)
    }

    Try(Await.result(runningServer, Duration.Inf)).toEither.map(_ => ())
  }
}

class JettyMockService[F[_]] private[service] (port: Int)(
    implicit val ec: ExecutionContext,
    mockServiceExecutor: MockServiceExecutor[F]
) extends LazyLogging {
  private[this] val requestMatching = new RequestMatching(new MatchingAttempt)
  private val server = new Server(port)

  private var runningServer: F[Server] = _

  def start(): Unit = {
    val server = new Server(port)
    server.setHandler(mockHandler)

    runningServer = mockServiceExecutor.run(server)
  }

  def reset(): Unit = requestMatching.reset()

  def addExpectation(expectation: ServiceExpectation): JettyMockService[F] = {
    requestMatching.addExpectation(expectation)
    this
  }

  def verifyCall(expectation: ServiceExpectation, expectedTimes: Int = 1): Unit =
    requestMatching.verifyCall(expectation, expectedTimes)

  def shutDown(): Unit = {
    mockServiceExecutor.shutDown(runningServer)
  }

  private lazy val mockHandler = new Handler {
    private var currentServer: Server = _
    private var running: Boolean = false
    private var stopped: Boolean = true

    override def handle(target: String,
                        baseRequest: org.eclipse.jetty.server.Request,
                        request: HttpServletRequest,
                        response: HttpServletResponse): Unit = {

      val matchableRequest = MatchableRequest.fromHttpServletRequest(baseRequest)
      println(requestMatching.toString)
      val potentialResponse = requestMatching
        .resolveResponse(matchableRequest)

      baseRequest.setHandled(true)
      val out = response.getOutputStream

      potentialResponse.maybeResponse match {
        case Some(matchedResponse: MatchedResponse) =>
          response.setStatus(matchedResponse.status.code)
          matchedResponse match {
            case matchedResponseWithPotentialBody: MatchedResponseWithPotentialBody =>
              matchedResponseWithPotentialBody.maybeBody.foreach(
                body => out.write(body.getBytes("UTF-8"))
              )

              matchedResponseWithPotentialBody.allHeaders.foreach { header =>
                response.setHeader(header.name.value, header.value)
              }

            case EmptyResponse(statusCode, customHeaders) =>
              response.setStatus(statusCode)
              customHeaders.foreach { header =>
                response.setHeader(header.name.value, header.value)
              }

            case LocationResponse(statusCode, uri, customHeaders) =>
              response.setStatus(statusCode)

              println(s"Location: ${uri}")
              response.setHeader("Location", uri)

              customHeaders.foreach { header =>
                response.setHeader(header.name.value, header.value)
              }
          }

        case None =>
          val unSuccessfulResponse =
            UnSuccessfulResponse(matchableRequest, potentialResponse.allAttempts)

          println(unSuccessfulResponse.prettyFormat)
          logger.warn(unSuccessfulResponse.prettyFormat)
          response.setStatus(501)
          out.write(unSuccessfulResponse.asErrorJson.spaces2.getBytes("UTF-8"))
      }
    }

    override def setServer(server: Server): Unit = {
      println(server)
      currentServer = server
    }

    override def getServer: Server = server

    override def destroy(): Unit = ()

    override def start(): Unit = {
      ()
    }

    override def stop(): Unit = ()

    override def isRunning: Boolean = running

    override def isStarted: Boolean = ???

    override def isStarting: Boolean = ???

    override def isStopping: Boolean = ???

    override def isStopped: Boolean = stopped

    override def isFailed: Boolean = ???

    override def addEventListener(listener: EventListener): Boolean = ???

    override def removeEventListener(listener: EventListener): Boolean = ???
  }
}
