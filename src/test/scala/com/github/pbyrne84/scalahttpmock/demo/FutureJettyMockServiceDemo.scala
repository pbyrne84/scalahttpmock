package com.github.pbyrne84.scalahttpmock.demo

import com.github.pbyrne84.scalahttpmock.service.implementations.JettyMockService

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object FutureJettyMockServiceDemo {

  private val port = 8080

  def main(args: Array[String]): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global

    val jettyMockService: JettyMockService[Future] = JettyMockService.createFutureVersion(port)

    val result = for {
      runningService <- jettyMockService.start()
      _ = DemoExpectations.expectations.map(jettyMockService.addExpectation)
      _ = Thread.sleep(60000)
      shutdown <- runningService.shutDown()

    } yield shutdown

    Await.result(result, Duration.Inf)

    System.exit(0)
  }

}
