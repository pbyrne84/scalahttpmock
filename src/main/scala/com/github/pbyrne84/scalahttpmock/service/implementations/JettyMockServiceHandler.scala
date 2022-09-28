package com.github.pbyrne84.scalahttpmock.service.implementations

import com.github.pbyrne84.scalahttpmock.expectation.{
  EmptyResponse,
  LocationResponse,
  MatchableRequest,
  MatchedResponse,
  MatchedResponseWithPotentialBody
}
import com.github.pbyrne84.scalahttpmock.service.request.UnSuccessfulResponse
import com.typesafe.scalalogging.LazyLogging
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.eclipse.jetty.server.{Handler, Server}

import java.util.EventListener

class JettyMockServiceHandler[F[_]](server: Server, jettyMockService: JettyMockService[F])
    extends Handler
    with LazyLogging {
  private var currentServer: Server = _
  private var running: Boolean = false
  private var stopped: Boolean = true

  override def handle(target: String,
                      baseRequest: org.eclipse.jetty.server.Request,
                      request: HttpServletRequest,
                      response: HttpServletResponse): Unit = {

    val matchableRequest: MatchableRequest = MatchableRequest.fromHttpServletRequest(baseRequest)
    val potentialResponse = jettyMockService
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
