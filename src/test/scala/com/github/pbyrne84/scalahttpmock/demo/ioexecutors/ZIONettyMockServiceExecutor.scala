package com.github.pbyrne84.scalahttpmock.demo.ioexecutors

import com.github.pbyrne84.scalahttpmock.service.executor.{NettyMockServiceExecutor, RunningServer}
import com.typesafe.scalalogging.StrictLogging
import io.netty.channel.{ChannelFuture, EventLoopGroup}
import zio.{Task, ZIO}

import java.util.concurrent.TimeUnit
import scala.util.Try

class ZIONettyMockServiceExecutor extends NettyMockServiceExecutor[Task] with StrictLogging {

  override def run(
      channelFuture: ChannelFuture,
      bossGroup: EventLoopGroup,
      workerGroup: EventLoopGroup
  ): Task[RunningServer[Task]] = {

    ZIO.attempt {
      logger.info("starting")

      channelFuture.sync.channel
      import zio.interop.catz._

      new RunningServer[Task] {
        override def shutDown: Task[Either[Throwable, Unit]] = {
          for {
            result <- ZIO.attempt(shutDownEventLoopGroups)
          } yield result
        }

        private def shutDownEventLoopGroups: Either[Throwable, Unit] = {
          for {
            _ <- Right(logger.info("shutting bossGroup"))
            _ <- Try(bossGroup.shutdownGracefully(1, 1, TimeUnit.SECONDS).sync.get).toEither
            _ <- Right(logger.info("shutting workerGroup"))
            _ <- Try(workerGroup.shutdownGracefully(1, 1, TimeUnit.SECONDS).sync.get).toEither
          } yield ()
        }

        override def wrapInTask[A](call: => A): Task[A] = ZIO.attempt(call)
      }
    }
  }

  override def wrapInEffect[A](call: => A): Task[A] =
    ZIO.attempt(call)

  override def wrapFailure[A](call: => Task[A])(errorRemapping: Throwable => Throwable): Task[A] = {
    call.mapError(error => errorRemapping(error))
  }
}
