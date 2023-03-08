package com.github.pbyrne84.scalahttpmock.demo.ioexecutors.netty

import io.netty.buffer.Unpooled
import io.netty.channel.{ChannelFutureListener, ChannelHandlerContext, EventLoopGroup, SimpleChannelInboundHandler}
import io.netty.handler.codec.http.HttpHeaderNames.{CONNECTION, CONTENT_LENGTH, CONTENT_TYPE}
import io.netty.handler.codec.http.HttpHeaderValues.{CLOSE, KEEP_ALIVE, TEXT_PLAIN}
import io.netty.handler.codec.http.HttpResponseStatus.OK
import io.netty.handler.codec.http._

import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

class HttpHelloWorldServerHandler(bossGroup: EventLoopGroup, workerGroup: EventLoopGroup)
    extends SimpleChannelInboundHandler[HttpObject] {
  private val CONTENT = "Hello World".getBytes

  override def channelReadComplete(ctx: ChannelHandlerContext): Unit = {
    ctx.flush
  }

  def channelRead0(ctx: ChannelHandlerContext, msg: HttpObject): Unit = {

    msg match {
      case req: FullHttpRequest =>
        println("bananana")
        printMessage(msg)
        val keepAlive = HttpUtil.isKeepAlive(req)
        val response =
          new DefaultFullHttpResponse(req.protocolVersion, OK, Unpooled.wrappedBuffer(CONTENT))

        response.headers
          .set(CONTENT_TYPE, TEXT_PLAIN)
          .setInt(CONTENT_LENGTH, response.content.readableBytes)

        println(
          "????? " + req.content.readCharSequence(req.content.readableBytes(), Charset.defaultCharset())
        )

        if (keepAlive)
          if (!req.protocolVersion.isKeepAliveDefault) response.headers.set(CONNECTION, KEEP_ALIVE)
          else {
            // Tell the client we're going to close the connection.
            response.headers.set(CONNECTION, CLOSE)
          }
        val f = ctx.write(response)
        if (!keepAlive) f.addListener(ChannelFutureListener.CLOSE)

        if (req.uri() == "/die") {
          println("bossgroup")
          bossGroup.shutdownGracefully(1, 1, TimeUnit.SECONDS).sync()
          println("workerGroup")
          workerGroup.shutdownGracefully(1, 1, TimeUnit.SECONDS).await(1)
          println("chupacabra")
        }

//      case a: LastHttpContent =>
//        printMessage(msg)
//

      case _ =>
        println(s"Unsupported type ${msg.getClass} - ${msg.toString}, expected HttpRequest")
    }
  }

  private def printMessage(msg: HttpObject) = {
    msg match {
      case a: DefaultHttpRequest =>
        a.decoderResult()
        println("----- " + a.getClass)
        println("----- " + a)
        println("***********************")

      case httpObject: DefaultHttpObject =>
        httpObject.decoderResult()
        println("aaaaa " + httpObject)
        println("***********************")
      case content: HttpContent =>
        println("bbbbb " + content)
      case message: HttpMessage =>
        println("ccccc " + message)
      case _ => println(msg.getClass)
    }

  }

  override def exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable): Unit = {
    cause.printStackTrace()
    ctx.close
  }
}
