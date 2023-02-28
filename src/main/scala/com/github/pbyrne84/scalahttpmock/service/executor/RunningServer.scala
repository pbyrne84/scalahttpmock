package com.github.pbyrne84.scalahttpmock.service.executor

trait RunningServer[F[_]] {

  def shutDown(): F[Either[Throwable, Unit]]
}
