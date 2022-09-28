package com.github.pbyrne84.scalahttpmock.demo.ioexecutors

import com.github.pbyrne84.scalahttpmock.service.executor.{MockServiceExecutor, RunningServer}
import org.eclipse.jetty.server.Server
import zio.ZIO

import scala.util.Try

class ZIOMockServiceExecutor extends MockServiceExecutor[zio.Task] {
  override def run(server: Server): zio.Task[RunningServer[zio.Task]] = {

    ZIO.attempt {
      println("starting")
      server.start()
      println("started")
      // This joins it to a thread and lock it up so kooky
      // server.join()

      new RunningServer[zio.Task] {
        override def shutDown(): zio.Task[Either[Throwable, Unit]] =
          ZIO.attempt {
            println("shutting down")
            Try(server.setStopAtShutdown(true)).toEither
          }
      }
    }
  }

}
