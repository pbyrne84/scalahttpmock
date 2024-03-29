package com.github.pbyrne84.scalahttpmock.service.executor

import io.netty.channel.{ChannelFuture, EventLoopGroup}

trait NettyMockServiceExecutor[F[_]] {

  def run(channelFuture: ChannelFuture, bossGroup: EventLoopGroup, workerGroup: EventLoopGroup): F[RunningServer[F]]

  def wrapInEffect[A](call: => A): F[A]

  def wrapFailure[A](call: => F[A])(errorRemapping: Throwable => Throwable): F[A]
}
