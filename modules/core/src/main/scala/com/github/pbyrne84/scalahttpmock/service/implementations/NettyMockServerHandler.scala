package com.github.pbyrne84.scalahttpmock.service.implementations

import com.github.pbyrne84.scalahttpmock.expectation.{
  Header,
  MatchableRequest,
  MatchedResponse,
  MatchedResponseWithPotentialBody
}
import com.github.pbyrne84.scalahttpmock.service.request.{RequestMatching, UnSuccessfulResponse}
import com.typesafe.scalalogging.StrictLogging
import io.netty.buffer.Unpooled
import io.netty.channel.{ChannelFutureListener, ChannelHandlerContext, SimpleChannelInboundHandler}
import io.netty.handler.codec.http._

class NettyMockServerHandler(requestMatching: RequestMatching)
    extends SimpleChannelInboundHandler[HttpObject]
    with StrictLogging {

  override def channelReadComplete(ctx: ChannelHandlerContext): Unit = {
    ctx.flush
  }

  override def channelRead0(ctx: ChannelHandlerContext, msg: HttpObject): Unit = {
    msg match {
      case req: FullHttpRequest =>
        val isKeepAliveDefault = req.protocolVersion.isKeepAliveDefault

        logger.debug(s"Received $req")

        val matchableRequest = MatchableRequest.fromNettyFullHttpRequest(req)
        val potentialResponse = requestMatching.resolveResponse(matchableRequest)

        potentialResponse.maybeResponse match {
          case Some(foundResponse: MatchedResponse) =>
            val nettyResponse =
              convertMatchToNettyResponse(foundResponse, req)

            writeResponseOut(ctx, isKeepAliveDefault, keepAlive = false, nettyResponse = nettyResponse)

          case None =>
            val unSuccessfulResponse =
              UnSuccessfulResponse(matchableRequest, potentialResponse.allAttempts)

            logger.warn(unSuccessfulResponse.prettyFormat)
            // make the 500 customisable though it is the saner default to 404 as 404 is used for
            // not authorised making logic go down the not authorised branch when that should
            // be a specifically set expectation not firing warnings off.
            val nonExpectedResponse =
              createNonExpectedResponse(req.protocolVersion, 501, unSuccessfulResponse.prettyFormat)

            println(unSuccessfulResponse.prettyFormat)

            writeResponseOut(ctx, isKeepAliveDefault = false, keepAlive = false, nettyResponse = nonExpectedResponse)

        }

      case _ =>
        val message = s"Expected FullHttpRequest (via HttpObjectAggregator channel handler) but got " +
          s"unsupported request type ${msg.getClass} - ${msg.toString}"

        logger.error(message)

        val errorResponse = createNonExpectedResponse(HttpVersion.HTTP_1_1, 500, message)
        writeResponseOut(ctx = ctx, isKeepAliveDefault = false, keepAlive = false, nettyResponse = errorResponse)
    }
  }

  private def convertMatchToNettyResponse(
      foundResponse: MatchedResponse,
      req: FullHttpRequest
  ): DefaultFullHttpResponse = {
    val nettyResponseStatus = HttpResponseStatus.valueOf(foundResponse.statusCode)
    val nettyHeadersHeaders: HttpHeaders = convertMatchedResponseHeaders(foundResponse.allHeaders)

    foundResponse match {
      case matchedResponseWithPotentialBody: MatchedResponseWithPotentialBody =>
        new DefaultFullHttpResponse(
          req.protocolVersion,
          nettyResponseStatus,
          Unpooled.wrappedBuffer(
            matchedResponseWithPotentialBody.maybeBody.getOrElse("").getBytes("UTF-8")
          ),
          nettyHeadersHeaders,
          EmptyHttpHeaders.INSTANCE // ?? trailing headers
        )

      case _ =>
        new DefaultFullHttpResponse(
          req.protocolVersion,
          nettyResponseStatus,
          Unpooled.wrappedBuffer("".getBytes),
          nettyHeadersHeaders,
          EmptyHttpHeaders.INSTANCE
        )
    }
  }

  // bit of a blast from the past fluent interfaces having inheritance issues
  private def convertMatchedResponseHeaders(customHeaders: Seq[Header]) = {
    customHeaders.foldLeft(
      new DefaultHttpHeaders(): HttpHeaders
    ) { case (allNettyHeaders: HttpHeaders, customHeader: Header) =>
      allNettyHeaders.add(customHeader.name.value, customHeader.value)
    }
  }

  private def writeResponseOut(
      ctx: ChannelHandlerContext,
      isKeepAliveDefault: Boolean,
      keepAlive: Boolean,
      nettyResponse: DefaultFullHttpResponse
  ): Unit = {
    if (keepAlive)
      if (!isKeepAliveDefault)
        nettyResponse.headers.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.KEEP_ALIVE)
      else {
        // Tell the client we're going to close the connection.
        nettyResponse.headers.set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
      }
    val f = ctx.write(nettyResponse)
    if (!keepAlive)
      f.addListener(ChannelFutureListener.CLOSE)
  }

  private def createNonExpectedResponse(protocolVersion: HttpVersion, statusCode: Int, message: String) = {
    new DefaultFullHttpResponse(
      protocolVersion,
      HttpResponseStatus.valueOf(statusCode),
      Unpooled.wrappedBuffer(message.getBytes),
      EmptyHttpHeaders.INSTANCE,
      EmptyHttpHeaders.INSTANCE
    )
  }

}
