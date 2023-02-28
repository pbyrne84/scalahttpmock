package com.github.pbyrne84.scalahttpmock.service.executor

import com.github.pbyrne84.scalahttpmock.service.implementations.NettyMockServer
import io.netty.channel.{ChannelFuture, EventLoopGroup}

import java.util.concurrent.TimeUnit
import scala.concurrent.{ExecutionContext, Future}

object FutureNettyMockServiceExecutor {

  def createFutureVersion(port: Int)(implicit ec: ExecutionContext): NettyMockServer[Future] = {
    val futureMockServiceExecutor = new FutureNettyMockServiceExecutor
    new NettyMockServer(port, futureMockServiceExecutor)
  }
}

class FutureNettyMockServiceExecutor(implicit val ec: ExecutionContext) extends NettyMockServiceExecutor[Future] {

  override def run(
      channelFuture: ChannelFuture,
      bossGroup: EventLoopGroup,
      workerGroup: EventLoopGroup
  ): Future[RunningServer[Future]] = {
    Future {
      println("starting")

      channelFuture.sync.channel

      new RunningServer[Future] {
        override def shutDown(): Future[Either[Throwable, Unit]] = {
          println("shutting down")
          (for {
            _ <- Future(bossGroup.shutdownGracefully(1, 1, TimeUnit.SECONDS).sync.get)
            _ = println("workerGroup")
            _ <- Future(workerGroup.shutdownGracefully(1, 1, TimeUnit.SECONDS).sync.get)
          } yield ())
            .map(Right.apply)
            .recover(e => Left(e))
        }
      }
    }
  }
}
