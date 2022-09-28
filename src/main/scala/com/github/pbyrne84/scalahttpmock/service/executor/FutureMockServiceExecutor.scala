package com.github.pbyrne84.scalahttpmock.service.executor

import org.eclipse.jetty.server.Server

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class FutureMockServiceExecutor(implicit val ec: ExecutionContext)
    extends MockServiceExecutor[Future] {

  override def run(server: Server): Future[RunningServer[Future]] = {
    Future {
      println("starting")
      server.start()

      new RunningServer[Future] {
        override def shutDown(): Future[Either[Throwable, Unit]] = {
          Future {
            Try {
              println("shutting down")
              server.setStopAtShutdown(true)
              println("shut down")
            }.toEither
          }
        }
      }
    }
  }

}
