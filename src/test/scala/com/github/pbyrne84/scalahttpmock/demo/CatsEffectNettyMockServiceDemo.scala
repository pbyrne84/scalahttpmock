package com.github.pbyrne84.scalahttpmock.demo

import cats.effect.{ExitCode, IO, IOApp}
import com.github.pbyrne84.scalahttpmock.demo.ioexecutors.CENettyMockServiceExecutor
import com.github.pbyrne84.scalahttpmock.service.implementations.NettyMockServer

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.FiniteDuration

object CatsEffectNettyMockServiceDemo extends IOApp {

  private val port = 8080

  override def run(args: List[String]): IO[ExitCode] = {

    val catsEffectMockServiceExecutor = new CENettyMockServiceExecutor()
    val jettyMockService =
      new NettyMockServer(port, catsEffectMockServiceExecutor)

    val eventualShutdownService: IO[Either[Throwable, Unit]] = for {
      runningService <- jettyMockService.start
      _ = DemoExpectations.expectations.map(jettyMockService.addExpectation)
      _ <- IO.sleep(FiniteDuration(length = 60, unit = TimeUnit.SECONDS))
      shutdownService <- runningService.shutDown
    } yield shutdownService

    eventualShutdownService.map {
      case Left(_) => ExitCode.Error
      case Right(_) => ExitCode.Success
    }
  }

}
