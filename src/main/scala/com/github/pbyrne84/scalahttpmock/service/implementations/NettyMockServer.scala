package com.github.pbyrne84.scalahttpmock.service.implementations

import com.github.pbyrne84.scalahttpmock.expectation.{MatchingAttempt, ServiceExpectation}
import com.github.pbyrne84.scalahttpmock.service.executor.{
  FutureNettyMockServiceExecutor,
  NettyMockServiceExecutor,
  RunningServer
}
import com.github.pbyrne84.scalahttpmock.service.request.RequestMatching
import com.typesafe.scalalogging.LazyLogging
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.{ChannelFuture, ChannelOption, EventLoopGroup}
import io.netty.handler.logging.{LogLevel, LoggingHandler}
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.SelfSignedCertificate

import scala.concurrent.{ExecutionContext, Future}

object NettyMockServer {

  def createFutureVersion(port: Int, bossGroupThreadCount: Int = 1)(implicit
      ec: ExecutionContext
  ): NettyMockServer[Future] = {
    val futureMockServiceExecutor = new FutureNettyMockServiceExecutor
    new NettyMockServer(port, futureMockServiceExecutor, bossGroupThreadCount)
  }
}

class NettyMockServer[F[_]](
    port: Int,
    nettyMockServiceExecutor: NettyMockServiceExecutor[F],
    bossGroupThreadCount: Int = 1
) extends LazyLogging {

  private val requestMatching = new RequestMatching(new MatchingAttempt)

  private val SSL: Boolean = false // System.getProperty("ssl") != null

  def start: F[RunningServer[F]] = {
    val bossGroup: EventLoopGroup = new NioEventLoopGroup(bossGroupThreadCount)
    val workerGroup: EventLoopGroup = new NioEventLoopGroup

    nettyMockServiceExecutor.run(
      createServerBootStrap(requestMatching, bossGroup, workerGroup),
      bossGroup,
      workerGroup
    )
  }

  private def createServerBootStrap(
      requestMatching: RequestMatching,
      bossGroup: EventLoopGroup,
      workerGroup: EventLoopGroup
  ): ChannelFuture = {

    val maybeSslContext = if (SSL) {
      val ssc = new SelfSignedCertificate
      Some(SslContextBuilder.forServer(ssc.certificate, ssc.privateKey).build)
    } else {
      None
    }

    val serverBootstrap = new ServerBootstrap
    serverBootstrap.option(ChannelOption.SO_BACKLOG, Integer.valueOf(1024))
    val initializer = new NettyMockServerInitializer(maybeSslContext, requestMatching)

    serverBootstrap
      .group(bossGroup, workerGroup)
      .channel(classOf[NioServerSocketChannel])
      .handler(new LoggingHandler(LogLevel.INFO))
      .childHandler(initializer)
      .bind(port)
  }

  def addExpectation(serviceExpectation: ServiceExpectation): Unit = {
    requestMatching.addExpectation(serviceExpectation)
  }

  def reset(): Unit = requestMatching.reset()

  def verifyCall(expectation: ServiceExpectation, expectedTimes: Int = 1): Unit =
    requestMatching.verifyCall(expectation, expectedTimes)
}
