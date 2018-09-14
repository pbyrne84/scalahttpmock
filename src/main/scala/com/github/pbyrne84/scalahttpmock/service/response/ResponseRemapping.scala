package com.github.pbyrne84.scalahttpmock.service.response

import cats.effect.IO
import com.github.pbyrne84.scalahttpmock.expectation.{
  AllMatchResult,
  EmptyResponse,
  LocationResponse,
  MatchedResponse,
  MatchedResponseWithPotentialBody
}
import com.github.pbyrne84.scalahttpmock.service.request.UnSuccessfulResponse
import com.typesafe.scalalogging.StrictLogging
import org.http4s.dsl.impl.{
  EmptyResponseGenerator,
  EntityResponseGenerator,
  LocationResponseGenerator
}
import org.http4s.headers.`Content-Type`
import org.http4s.{MediaType, Request, Response, Status}

import scala.collection.immutable.Seq

object ResponseRemapping extends StrictLogging {
  import org.http4s.dsl.io._

  class EntityResponseOps[F[_]](val status: Status) extends AnyVal with EntityResponseGenerator[F]
  class EmptyResponseOps[F[_]](val status: Status) extends AnyVal with EmptyResponseGenerator[F]
  class LocationResponseOps[F[_]](val status: Status)
      extends AnyVal
      with LocationResponseGenerator[F]

  private[scalahttpmock] def respondSuccessfully(
      matchedResponse: MatchedResponse
  ): IO[Response[IO]] = {
    val finalResponse: IO[Response[IO]] = matchedResponse match {
      case responseWithPotentialBody: MatchedResponseWithPotentialBody =>
        responseWithPotentialBody.maybeBody match {
          case Some(body) =>
            new EntityResponseOps(matchedResponse.status)
              .apply(body)

          case _ =>
            IO(Response(matchedResponse.status))

        }

      case emptyResponse: EmptyResponse =>
        new EmptyResponseOps(emptyResponse.status)
          .apply()

      case locationResponse: LocationResponse =>
        IO(Response(locationResponse.status))

      case _ =>
        IO(Response(matchedResponse.status))
    }

    finalResponse.putHeaders(matchedResponse.allHeaders: _*)
  }

  private[scalahttpmock] def respondUnSuccessfully(
      request: Request[IO],
      allAttempts: Seq[AllMatchResult]
  ): IO[Response[IO]] = {
    val unSuccessfulResponse = UnSuccessfulResponse(request, allAttempts)
    logger.warn(unSuccessfulResponse.prettyFormat)

    val httpResponse: IO[Response[IO]] = new EntityResponseOps(Status.NotImplemented)
      .apply(unSuccessfulResponse.asErrorJson.spaces2)

    //Type is mangled if this is joined by a fluent interface
    httpResponse.putHeaders(`Content-Type`(MediaType.`application/json`))
  }

}
