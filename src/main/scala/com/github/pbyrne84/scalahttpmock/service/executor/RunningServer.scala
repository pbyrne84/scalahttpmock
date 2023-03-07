package com.github.pbyrne84.scalahttpmock.service.executor

import cats.Monad

abstract class RunningServer[F[_]: Monad] {

  def shutDown: F[Either[Throwable, Unit]]

  def wrapInTask[A](call: => A): F[A]

}
