package com.github.pbyrne84.scalahttpmock.demo

import com.github.pbyrne84.scalahttpmock.demo.ioexecutors.ZIONettyMockServiceExecutor
import com.github.pbyrne84.scalahttpmock.service.implementations.NettyMockServer
import zio.{ZIO, ZIOAppDefault}

import java.time.Duration

object ZioNettyMockServiceDemo extends ZIOAppDefault {

  private val port = 8080

  override def run = {

    val zIOMockServiceExecutor = new ZIONettyMockServiceExecutor()
    val jettyMockService =
      new NettyMockServer(port, zIOMockServiceExecutor)
    for {
      runningService <- jettyMockService.start
      _ = DemoExpectations.expectations.map(jettyMockService.addExpectation)
      _ <- ZIO.sleep(Duration.ofSeconds(60))
      shutdownService <- runningService.shutDown()
    } yield shutdownService
  }

}
