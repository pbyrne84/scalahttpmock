package com.github.pbyrne84.scalahttpmock.demo

import com.github.pbyrne84.scalahttpmock.service.executor.FutureNettyMockServiceExecutor
import com.github.pbyrne84.scalahttpmock.service.implementations.NettyMockServer

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object FutureNettyMockServiceDemo {

  private val port = 8080

  def main(args: Array[String]): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val nettyMockService: NettyMockServer[Future] =
      FutureNettyMockServiceExecutor.createFutureVersion(port)

    val result = for {
      runningService <- nettyMockService.start
      _ = DemoExpectations.expectations.map(nettyMockService.addExpectation)
      _ = Thread.sleep(6000)
      shutdown <- runningService.shutDown

    } yield shutdown

    Await.result(result, Duration.Inf)
  }

}
