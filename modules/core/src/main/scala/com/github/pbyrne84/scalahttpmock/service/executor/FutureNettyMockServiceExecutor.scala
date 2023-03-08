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
        override def shutDown: Future[Either[Throwable, Unit]] = {
          (for {
            _ <- Future.successful(println("shutting down bossGroup"))
            _ <- Future(bossGroup.shutdownGracefully(1, 1, TimeUnit.SECONDS).sync.get)
            _ <- Future.successful(println("shutting down workerGroup"))
            _ <- Future(workerGroup.shutdownGracefully(1, 1, TimeUnit.SECONDS).sync.get)
          } yield ())
            .map(Right.apply)
            .recover(e => Left(e))
        }

        override def wrapInTask[A](call: => A): Future[A] = Future(call)
      }
    }
  }

  override def wrapInEffect[A](call: => A): Future[A] = Future(call)

  override def wrapFailure[A](call: => Future[A])(errorRemapping: Throwable => Throwable): Future[A] = {
    call.recoverWith { case error: Throwable =>
      Future.failed(errorRemapping(error))
    }
  }
}
