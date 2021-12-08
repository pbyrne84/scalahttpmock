package com.github.pbyrne84.scalahttpmock.service

import zhttp.http._
import zhttp.service._
import zio._

object HelloWorldAdvanced extends App {
  // Set a port
  private val PORT = 8090

  // Create HTTP route
  val app: HttpApp[Any, Nothing] = HttpApp.collect {
    case request => Response.text(s"Hello World! $request")
  }

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    // Configure thread count using CLI
    Server.start(PORT, app).exitCode
  }
}
