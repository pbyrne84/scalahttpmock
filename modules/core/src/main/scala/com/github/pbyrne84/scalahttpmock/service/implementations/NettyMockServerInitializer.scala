package com.github.pbyrne84.scalahttpmock.service.implementations

import com.github.pbyrne84.scalahttpmock.service.request.RequestMatching
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.{HttpObjectAggregator, HttpServerCodec}
import io.netty.handler.ssl.SslContext

class NettyMockServerInitializer(
    maybeSslCtx: Option[SslContext],
    requestMatching: RequestMatching,
    maxContentLength: Int = 65536
) extends ChannelInitializer[SocketChannel] {

  override def initChannel(ch: SocketChannel): Unit = {
    val p = ch.pipeline

    maybeSslCtx.foreach(_.newHandler(ch.alloc))

    p.addLast(new HttpServerCodec)
    // Aggregation forces complete message to the handler, else it comes in 2 calls, one with basic request info
    // and another with the body
    p.addLast(new HttpObjectAggregator(maxContentLength))
    p.addLast(new NettyMockServerHandler(requestMatching))
  }
}
