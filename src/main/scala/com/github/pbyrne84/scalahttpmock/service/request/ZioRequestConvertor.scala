package com.github.pbyrne84.scalahttpmock.service.request

import cats.effect.IO
import io.netty.handler.codec.http.HttpResponseStatus
import org.http4s
import org.http4s.Uri
import zhttp.http.{Header, Method, Request, Status}

class ZioRequestConvertor {
  def convert(zRequest: Request): org.http4s.Request[IO] = {
    val zheaders: List[Header] = zRequest.headers
    val content = zRequest.getBodyAsString.getOrElse("")

    val http4sHeaders = zheaders
      .map(zHeader => org.http4s.Header(zHeader.name.toString, zHeader.value.toString))

    val http4sMethod: http4s.Method = zRequest.method match {
      case Method.OPTIONS => org.http4s.Method.OPTIONS
      case Method.GET => org.http4s.Method.GET
      case Method.HEAD => org.http4s.Method.HEAD
      case Method.POST => org.http4s.Method.POST
      case Method.PUT => org.http4s.Method.PUT
      case Method.PATCH => org.http4s.Method.PATCH
      case Method.DELETE => org.http4s.Method.DELETE
      case Method.TRACE => org.http4s.Method.TRACE
      case Method.CONNECT => org.http4s.Method.CONNECT
      case Method.CUSTOM(name) =>
        org.http4s.Method.fromString(name).getOrElse(org.http4s.Method.PUT)
    }

    createRequest
      .withUri(Uri.unsafeFromString(zRequest.url.asString))
      .withMethod(http4sMethod)
      .withHeaders(http4sHeaders: _*)
      .withBody(content)
      .unsafeRunSync()
  }

  private def createRequest: org.http4s.Request[IO] = org.http4s.Request()

  def http4StatusToZHTTPStatus(statusInt: Int): Status =
    Status.fromJHttpResponseStatus(HttpResponseStatus.valueOf(statusInt))

}
