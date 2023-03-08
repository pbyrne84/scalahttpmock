package com.github.pbyrne84.scalahttpmock.zio.demo

import com.github.pbyrne84.scalahttpmock.demo.DemoExpectations
import com.github.pbyrne84.scalahttpmock.service.implementations.NettyMockServer
import com.github.pbyrne84.scalahttpmock.zio.ZIONettyMockServiceExecutor
import zio.{ZIO, ZIOAppDefault}

import java.time.Duration

object ZioNettyMockServiceDemo extends ZIOAppDefault {

  private val port = 8080

  override def run = {

    import zio.interop.catz._

    val zIOMockServiceExecutor = new ZIONettyMockServiceExecutor()
    val jettyMockService =
      new NettyMockServer(port, zIOMockServiceExecutor)
    for {
      runningService <- jettyMockService.start
      _ = DemoExpectations.expectations.map(jettyMockService.addExpectation)
      _ <- ZIO.sleep(Duration.ofSeconds(60))
      shutdownService <- runningService.shutDown
    } yield shutdownService
  }

}
