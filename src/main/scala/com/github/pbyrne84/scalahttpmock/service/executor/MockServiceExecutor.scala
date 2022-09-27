package com.github.pbyrne84.scalahttpmock.service.executor

import org.eclipse.jetty.server.Server

trait RunningServer[F[_]] {

  def shutDown(): F[Either[Throwable, Unit]]
}

trait MockServiceExecutor[F[_]] {

  def run(server: Server): F[RunningServer[F]]
}
