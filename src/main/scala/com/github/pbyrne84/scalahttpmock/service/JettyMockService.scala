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
import org.eclipse.jetty.util.thread.QueuedThreadPool

import java.util.EventListener
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object JettyMockService {
  def create(port: Int): JettyMockService = new JettyMockService(port)
}

class JettyMockService private[service] (port: Int) extends LazyLogging {
  private[this] val requestMatching = new RequestMatching(new MatchingAttempt)
  val server = new Server(port)

  Future {
    server.getThreadPool.asInstanceOf[QueuedThreadPool].setMaxThreads(100)
    server.setHandler(mockHandler)
    server.start()
    server.join()
  }

  def reset(): Unit = requestMatching.reset()

  def addExpectation(expectation: ServiceExpectation): JettyMockService = {
    requestMatching.addExpectation(expectation)
    this
  }

  def verifyCall(expectation: ServiceExpectation, expectedTimes: Int = 1): Unit =
    requestMatching.verifyCall(expectation, expectedTimes)

  def shutDown(): Unit = {} //server.shutdownNow()

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

              println(s"Location: ${uri.renderString}")
              response.setHeader("Location", uri.renderString)

              customHeaders.foreach { header =>
                response.setHeader(header.name.value, header.value)
              }
          }

        case None =>
          val unSuccessfulResponse =
            UnSuccessfulResponse(matchableRequest, potentialResponse.allAttempts)

          println(unSuccessfulResponse.prettyFormat)
          logger.warn(unSuccessfulResponse.prettyFormat)
          response.setStatus(HttpServletResponse.SC_OK)
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
