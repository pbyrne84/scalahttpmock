package com.github.pbyrne84.scalahttpmock

import com.github.pbyrne84.scalahttpmock.expectation.MatchableRequest
import jakarta.servlet.http.{HttpServletRequest, HttpServletResponse}
import org.eclipse.jetty.server.{Handler, Request, Server}

import java.util.EventListener
import scala.jdk.CollectionConverters.MapHasAsScala

object JettyTest {

  def main(args: Array[String]): Unit = {

    println("http://localhost:8080/a/v/s/c?a=1)".replaceAll("(http(s*))://localhost:\\d+", ""))

    val port = System.getProperty("port", "8080").toInt
    val server = new Server(port)

    //  server.setHandler(context)
    server.setHandler(a)

    server.start()
    server.join()
  }

  private val a = new Handler {
    private var currentServer: Server = _
    private var running: Boolean = false
    private var stopped: Boolean = true

    override def handle(target: String,
                        baseRequest: Request,
                        request: HttpServletRequest,
                        response: HttpServletResponse): Unit = {

      println(s"target=" + target)
      println(baseRequest.getRequestURL.toString)
      println(baseRequest.toString)
      println(request.getMethod)
      println(request)
      println(response)
      println(baseRequest.getParameterMap.asScala.foreach {
        case (a, b) =>
          println(s"$a=${b.toList}")
      })

      baseRequest.getServletContext

      response.setStatus(200)

      request.getContentLength

      import jakarta.servlet.http.HttpServletResponse
      response.setContentType("text/html;charset=utf-8")
      response.setStatus(HttpServletResponse.SC_OK)
      baseRequest.setHandled(true)
      val out = response.getOutputStream
      out.write("bananana".getBytes())

      import com.github.pbyrne84.scalahttpmock.expectation.RequestPrettification._

      println(MatchableRequest.fromHttpServletRequest(baseRequest).prettyFormat)

    }

    override def setServer(server: Server): Unit = {
      println(server)
      currentServer = server
    }

    override def getServer: Server = ???

    override def destroy(): Unit = ???

    override def start(): Unit = {
      ()
    }

    override def stop(): Unit = ???

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
