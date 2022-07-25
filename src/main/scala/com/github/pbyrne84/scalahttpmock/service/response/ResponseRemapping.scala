package com.github.pbyrne84.scalahttpmock.service.response

import cats.effect.IO
import com.github.pbyrne84.scalahttpmock.expectation.{
  AllMatchResult,
  EmptyResponse,
  LocationResponse,
  MatchableRequest,
  MatchedResponse,
  MatchedResponseWithPotentialBody
}
import com.github.pbyrne84.scalahttpmock.service.request.UnSuccessfulResponse
import com.typesafe.scalalogging.StrictLogging
import fs2.{text, Stream}
import org.http4s.headers.`Content-Type`
import org.http4s.{Headers, MediaType, Response, Status}

object ResponseRemapping extends StrictLogging {

  private[scalahttpmock] def respondSuccessfully(
      matchedResponse: MatchedResponse
  ): IO[Response[IO]] = {
    val finalResponse: IO[Response[IO]] = matchedResponse match {
      case responseWithPotentialBody: MatchedResponseWithPotentialBody =>
        responseWithPotentialBody.maybeBody match {
          case Some(body) =>
            IO(Response(matchedResponse.status, body = Stream(body).through(text.utf8Encode)))

          case _ =>
            IO(Response(matchedResponse.status))

        }

      case emptyResponse: EmptyResponse =>
        IO(Response(emptyResponse.status))

      case locationResponse: LocationResponse =>
        IO(Response(locationResponse.status))

      case _ =>
        IO(Response(matchedResponse.status))
    }

    finalResponse.map(_.copy(headers = Headers.of(matchedResponse.allHeaders: _*)))

  }

  private[scalahttpmock] def respondUnSuccessfully(
      request: MatchableRequest,
      allAttempts: Seq[AllMatchResult]
  ): IO[Response[IO]] = {
    val unSuccessfulResponse = UnSuccessfulResponse(request, allAttempts)
    logger.warn(unSuccessfulResponse.prettyFormat)

    val httpResponse: IO[Response[IO]] =
      IO(
        Response(Status.NotImplemented,
                 body = Stream(unSuccessfulResponse.asErrorJson.spaces2).through(text.utf8Encode))
      )

    //Type is mangled if this is joined by a fluent interface
    httpResponse.map(_.copy(headers = Headers.of(`Content-Type`(MediaType.application.json))))
  }

}
