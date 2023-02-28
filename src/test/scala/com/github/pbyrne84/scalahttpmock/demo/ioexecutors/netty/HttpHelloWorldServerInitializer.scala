package com.github.pbyrne84.scalahttpmock.demo.ioexecutors.netty

import io.netty.channel.socket.SocketChannel
import io.netty.channel.{ChannelInitializer, EventLoopGroup}
import io.netty.handler.codec.http.{HttpObjectAggregator, HttpServerCodec}
import io.netty.handler.ssl.SslContext

class HttpHelloWorldServerInitializer(sslCtx: SslContext, bossGroup: EventLoopGroup, workerGroup: EventLoopGroup)
    extends ChannelInitializer[SocketChannel] {

  def initChannel(ch: SocketChannel): Unit = {
    val p = ch.pipeline

    if (sslCtx != null)
      p.addLast(sslCtx.newHandler(ch.alloc))

    p.addLast(new HttpServerCodec)
    p.addLast(new HttpObjectAggregator(65536))
    p.addLast(new HttpHelloWorldServerHandler(bossGroup, workerGroup))
  }
}
