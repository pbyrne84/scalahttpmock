package com.github.pbyrne84.scalahttpmock.demo

import com.github.pbyrne84.scalahttpmock.demo.ioexecutors.ZIONettyMockServiceExecutor
import com.github.pbyrne84.scalahttpmock.expectation.ServiceExpectation
import com.github.pbyrne84.scalahttpmock.service.executor.RunningMockServerWithOperations
import com.github.pbyrne84.scalahttpmock.service.implementations.NettyMockServer
import zio.{Task, ZIO, ZLayer}

trait ZIOServiced[A] {
  protected val serviced: ZIO.ServiceWithZIOPartiallyApplied[A] = ZIO.serviceWithZIO[A]
}

/** We are going to use ZIO layers so in theory 1 service can be used across all tests. If this is not done then we can
  * have fun problems with the service trying to restart when it never shut down. For anything network related (DBs etc)
  * best to always do the simplest thing that cannot go wrong and leave the effort to be pointed at production code.
  */
object ZioNettyMockServer extends ZIOServiced[RunningMockServerWithOperations[Task]] {

  def layer(port: Int): ZLayer[Any, Throwable, RunningMockServerWithOperations[Task]] = ZLayer.apply {
    import zio.interop.catz._
    implicit val zIOMockServiceExecutor: ZIONettyMockServiceExecutor = new ZIONettyMockServiceExecutor()

    NettyMockServer.createIoMonadVersion(port).start.map { result =>
      println(s"starting zio server on $port")
      result
    }
  }

  def shutDown: ZIO[RunningMockServerWithOperations[Task], Throwable, Either[Throwable, Unit]] =
    serviced(_.shutDown)

  def reset: ZIO[RunningMockServerWithOperations[Task], Throwable, Unit] =
    serviced(_.reset)

  def addExpectation(
      serviceExpectation: ServiceExpectation
  ): ZIO[RunningMockServerWithOperations[Task], Throwable, Unit] =
    serviced(_.addExpectation(serviceExpectation))

  def verifyCall(
      expectation: ServiceExpectation,
      expectedTimes: Int = 1
  ): ZIO[RunningMockServerWithOperations[Task], Throwable, Unit] =
    serviced(_.verifyCall(expectation, expectedTimes))

}
