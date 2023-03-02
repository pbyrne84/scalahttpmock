package com.github.pbyrne84.scalahttpmock.demo.ioexecutors.netty

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.{Channel, ChannelOption, EventLoopGroup}
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.handler.logging.{LogLevel, LoggingHandler}
import io.netty.handler.ssl.{SslContext, SslContextBuilder}
import io.netty.handler.ssl.util.SelfSignedCertificate

import java.util.concurrent.TimeUnit;

//http://www.mastertheboss.com/jboss-frameworks/netty/how-to-create-an-http-server-with-netty/
object HttpHelloWorldServer {

  val SSL: Boolean = System.getProperty("ssl") != null
  val PORT: Int = System
    .getProperty(
      "port",
      if (SSL) "8443"
      else "8080"
    )
    .toInt
  @throws[Exception]
  def main(args: Array[String]): Unit = {
    // Configure SSL.
    var sslCtx: SslContext = null
    if (SSL) {
      val ssc = new SelfSignedCertificate
      sslCtx = SslContextBuilder.forServer(ssc.certificate, ssc.privateKey).build
    } else sslCtx = null
    // Configure the server.
    val bossGroup: EventLoopGroup = new NioEventLoopGroup(1)
    val workerGroup: EventLoopGroup = new NioEventLoopGroup
    try {
      val serverBootstrap = new ServerBootstrap
      serverBootstrap.option(ChannelOption.SO_BACKLOG, Integer.valueOf(1024))
      val initializer = new HttpHelloWorldServerInitializer(sslCtx, bossGroup, workerGroup)

      serverBootstrap
        .group(bossGroup, workerGroup)
        .channel(classOf[NioServerSocketChannel])
        .handler(new LoggingHandler(LogLevel.INFO))
        .childHandler(initializer)

      val channel: Channel = serverBootstrap.bind(PORT).sync.channel
      System.err.println(
        "Open your web browser and navigate to " + (if (SSL) "https"
                                                    else "http") + "://127.0.0.1:" + PORT + '/'
      )

      // bossGroup.shutdownGracefully(1, 1, TimeUnit.SECONDS).sync()
      // workerGroup.shutdownGracefully(1, 1, TimeUnit.SECONDS).sync()
      println("woof")
      val sync = channel.closeFuture.sync

      println(s"meow ${sync.isDone}")
    } finally {
      bossGroup.shutdownGracefully(1, 1, TimeUnit.SECONDS)
      workerGroup.shutdownGracefully(1, 1, TimeUnit.SECONDS)
    }
  }

}
