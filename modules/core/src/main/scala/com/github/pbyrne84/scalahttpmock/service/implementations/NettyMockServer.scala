package com.github.pbyrne84.scalahttpmock.service.implementations

import cats.Monad
import com.github.pbyrne84.scalahttpmock.expectation.{MatchingAttempt, ServiceExpectation}
import com.github.pbyrne84.scalahttpmock.service.executor.{
  FutureNettyMockServiceExecutor,
  NettyMockServiceExecutor,
  RunningMockServerWithOperations
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
import io.netty.util.internal.logging.{InternalLoggerFactory, Slf4JLoggerFactory}

import scala.concurrent.{ExecutionContext, Future}

object NettyMockServer {

  def createFutureVersion(port: Int, bossGroupThreadCount: Int = 1)(implicit
      ec: ExecutionContext
  ): NettyMockServer[Future] = {
    val futureMockServiceExecutor = new FutureNettyMockServiceExecutor
    new NettyMockServer(port, futureMockServiceExecutor, bossGroupThreadCount)
  }

  def createIoMonadVersion[F[_]](port: Int, bossGroupThreadCount: Int = 1)(implicit
      nettyMockServiceExecutor: NettyMockServiceExecutor[F],
      monad: Monad[F]
  ): NettyMockServer[F] = {
    new NettyMockServer(port, nettyMockServiceExecutor, bossGroupThreadCount)
  }
}

class NettyMockServer[F[_]: Monad](
    port: Int,
    nettyMockServiceExecutor: NettyMockServiceExecutor[F],
    bossGroupThreadCount: Int = 1
) extends LazyLogging {

  InternalLoggerFactory.setDefaultFactory(Slf4JLoggerFactory.INSTANCE)

  // Always forget this little thing as in a lot of examples it is not shown, hidden with rest of imports.
  // Best to keep it eyeball level as much as possible.
  import cats.implicits._

  private val requestMatching = new RequestMatching(new MatchingAttempt)

  private val SSL: Boolean = false // System.getProperty("ssl") != null

  def start: F[RunningMockServerWithOperations[F]] = {
    val bossGroup: EventLoopGroup = new NioEventLoopGroup(bossGroupThreadCount)
    val workerGroup: EventLoopGroup = new NioEventLoopGroup

    nettyMockServiceExecutor.wrapFailure(
      (for {
        _ <- nettyMockServiceExecutor.wrapInEffect(requestMatching.reset())
        runningService <- nettyMockServiceExecutor.run(
          createServerBootStrap(requestMatching, bossGroup, workerGroup),
          bossGroup,
          workerGroup
        )
      } yield runningService).map(service => new RunningMockServerWithOperations(service, requestMatching))
    )(errorRemapping = (error: Throwable) => new RuntimeException(s"Could not start service on port $port", error))

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

  @deprecated("Only used for examples")
  def addExpectation(serviceExpectation: ServiceExpectation): Unit = {
    requestMatching.addExpectation(serviceExpectation)
  }
}
