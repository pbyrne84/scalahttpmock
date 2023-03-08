package com.github.pbyrne84.scalahttpmock.expectation

import io.netty.handler.codec.http.FullHttpRequest

import java.net.URI
import scala.jdk.CollectionConverters.CollectionHasAsScala
case class MatchableRequest(
    uriPath: String,
    asPathWithParams: String,
    headers: List[Header],
    method: MockHttpMethod,
    multiParams: Map[String, Seq[String]],
    maybeContentAsString: Option[String],
    uri: String
)

object MatchableRequest {

  def fromNettyFullHttpRequest(request: FullHttpRequest): MatchableRequest = {
    val nettyHeaders = request.headers()
    val headers = nettyHeaders
      .names()
      .asScala
      .flatMap { headerName =>
        val headersValues = nettyHeaders.getAll(headerName).asScala.toList
        headersValues.map { headerValue =>
          Header(CaseInsensitiveString(headerName), headerValue)
        }
      }
      .toList

    val method = MockHttpMethod
      .fromString(request.method().name())
      .getOrElse(throw new RuntimeException(s"${request.method()} method cannot be mapped"))

    val rawUri = new URI(request.uri())
    val multiParams: Map[String, List[String]] = convertQuery(rawUri)

    val maybeContentAsString = if (request.content().readableBytes() > 0) {
      // this is fun https://stackoverflow.com/questions/7707556/how-to-convert-charsequence-to-string
      Some(
        request.content
          .readCharSequence(request.content.readableBytes(), sun.nio.cs.UTF_8.INSTANCE)
          .toString
      )
    } else {
      None
    }

    MatchableRequest(
      uriPath = rawUri.getPath,
      asPathWithParams = request.uri(),
      headers = headers,
      method = method,
      multiParams = multiParams,
      maybeContentAsString = maybeContentAsString,
      uri = request.uri()
    )

  }

  private def convertQuery(rawUri: URI): Map[String, List[String]] = {
    Option(rawUri.getRawQuery)
      .map { rawQuery =>
        rawQuery
          .split("&")
          .toList
          .flatMap { queryPart =>
            queryPart.split("=", 2).toList match {
              case ::(head, next) => Some(head -> next.mkString(""))
              case Nil => None
            }
          }
          .groupBy(_._1)
          .map { case (name, nameWithValues) => name -> nameWithValues.map(_._2) }
      }
      .getOrElse(Map.empty)
  }
}
