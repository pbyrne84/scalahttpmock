package com.github.pbyrne84.scalahttpmock.service

import cats.effect.IO
import com.github.pbyrne84.scalahttpmock.expectation.{
  EmptyResponse,
  LocationResponse,
  MatchedResponse,
  MatchedResponseWithPotentialBody
}
import org.http4s.dsl.impl.{
  EmptyResponseGenerator,
  EntityResponseGenerator,
  LocationResponseGenerator
}
import org.http4s.{Response, Status}

import scala.language.higherKinds

object ResponseRemapping {
  import org.http4s.dsl.io._

  class EntityResponseOps[F[_]](val status: Status) extends AnyVal with EntityResponseGenerator[F]
  class EmptyResponseOps[F[_]](val status: Status) extends AnyVal with EmptyResponseGenerator[F]
  class LocationResponseOps[F[_]](val status: Status)
      extends AnyVal
      with LocationResponseGenerator[F]

  def respond(matchedResponse: MatchedResponse): IO[Response[IO]] = {
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

}
