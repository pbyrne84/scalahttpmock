package org.scalahttpmock.service

import cats.effect.IO
import org.http4s.dsl.impl.EntityResponseGenerator
import org.http4s.{ParseFailure, Response, Status}
import org.scalahttpmock.expectation.MatchedResponse

import scala.language.higherKinds

object ResponseRemapping {
  import org.http4s.dsl.io._

  class EntityResponseOps[F[_]](val status: Status) extends AnyVal with EntityResponseGenerator[F]

  def respond(matchedResponse: MatchedResponse): IO[Response[IO]] = {

    val status = Status.fromInt(matchedResponse.statusCode) match {
      case Right(mappedStatus) => mappedStatus
      case Left(x: ParseFailure) => throw x
    }

    val finalResponse: IO[Response[IO]] = matchedResponse.statusCode match {
      case 200 | 201 | 202 | 203 | 404 =>
        new EntityResponseOps(status)
          .apply(matchedResponse.maybeBody.getOrElse(""))

    }

    finalResponse.putHeaders(matchedResponse.allHeaders: _*)
  }

}
