package com.github.pbyrne84.scalahttpmock.demo

import com.github.pbyrne84.scalahttpmock.demo.ioexecutors.ZIOMockServiceExecutor
import com.github.pbyrne84.scalahttpmock.service.implementations.JettyMockService
import zio.{Task, ZIO, ZIOAppDefault}

import java.time.Duration

object ZioJettyMockServiceDemo extends ZIOAppDefault {

  private val port = 8080

  override def run = {

    val zIOMockServiceExecutor = new ZIOMockServiceExecutor()
    val jettyMockService: JettyMockService[Task] =
      new JettyMockService(port, zIOMockServiceExecutor)

    for {
      runningService <- jettyMockService.start()
      _ = DemoExpectations.expectations.map(jettyMockService.addExpectation)
      _ <- ZIO.sleep(Duration.ofSeconds(60))
      shutdownService <- runningService.shutDown()
    } yield shutdownService
  }

}
