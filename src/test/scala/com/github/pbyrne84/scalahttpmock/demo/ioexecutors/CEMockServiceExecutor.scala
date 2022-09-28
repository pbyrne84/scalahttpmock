package com.github.pbyrne84.scalahttpmock.demo.ioexecutors

import cats.effect.IO
import com.github.pbyrne84.scalahttpmock.service.executor.{MockServiceExecutor, RunningServer}
import com.typesafe.scalalogging.StrictLogging
import org.eclipse.jetty.server.Server

import scala.util.Try

class CEMockServiceExecutor extends MockServiceExecutor[IO] with StrictLogging {
  override def run(server: Server): IO[RunningServer[IO]] = {

    IO {
      println("starting")
      server.start()
      println("started")

      new RunningServer[IO] {
        override def shutDown(): IO[Either[Throwable, Unit]] =
          IO {
            println("shutting down")
            Try(server.setStopAtShutdown(true)).toEither
          }
      }
    }
  }

}
