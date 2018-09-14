package com.github.pbyrne84.scalahttpmock.service.request

import cats.effect.IO
import com.github.pbyrne84.scalahttpmock.expectation.{
  AllMatchResult,
  Indentation,
  RequestPrettification
}
import io.circe.generic.semiauto.deriveEncoder
import io.circe.{Encoder, Json}
import org.http4s.Request

import scala.collection.immutable.Seq

case class UnSuccessfulResponse(request: Request[IO], allAttempts: Seq[AllMatchResult])
    extends Indentation {
  import RequestPrettification._

  private implicit val testResponseEncoder: Encoder[InvalidMatchResponse] =
    deriveEncoder[InvalidMatchResponse]

  case class InvalidMatchResponse(failedResponse: String)

  lazy val prettyFormat: String = {
    val requestText = indentNewLines(2, request.prettyFormat, indentFirstLine = true)

    val attemptedText =
      indentNewLines(4,
                     allAttempts
                       .sortBy(_.score.percentage)
                       .reverse
                       .map(_.prettifyScoreWithFailedOverview)
                       .mkString("\n\n"),
                     indentFirstLine = true)

    s"""
      |The Request was not matched :-
      |$requestText
      |  There was ${allAttempts.size} expectations :-
      |$attemptedText
    """.stripMargin.trim
  }

  lazy val asErrorJson: Json = {
    import io.circe.syntax._
    InvalidMatchResponse(prettyFormat).asJson
  }
}
