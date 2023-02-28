package com.github.pbyrne84.scalahttpmock.demo.ioexecutors

import cats.effect.IO
import com.github.pbyrne84.scalahttpmock.service.executor.{NettyMockServiceExecutor, RunningServer}
import com.typesafe.scalalogging.StrictLogging
import io.netty.channel.{ChannelFuture, EventLoopGroup}

import java.util.concurrent.TimeUnit

class CENettyMockServiceExecutor extends NettyMockServiceExecutor[IO] with StrictLogging {

  override def run(
      channelFuture: ChannelFuture,
      bossGroup: EventLoopGroup,
      workerGroup: EventLoopGroup
  ): IO[RunningServer[IO]] = {

    IO {
      logger.info("starting")

      channelFuture.sync.channel

      new RunningServer[IO] {
        override def shutDown(): IO[Either[Throwable, Unit]] = {
          println("shutting down")
          (for {
            _ <- IO(logger.info("shutting bossGroup"))
            _ <- IO(bossGroup.shutdownGracefully(1, 1, TimeUnit.SECONDS).sync.get)
            _ <- IO(logger.info("shutting workerGroup"))
            _ <- IO(workerGroup.shutdownGracefully(1, 1, TimeUnit.SECONDS).sync.get)
          } yield ())
            .map(Right.apply)
            .handleError(e => Left(e))
        }
      }
    }
  }
}
