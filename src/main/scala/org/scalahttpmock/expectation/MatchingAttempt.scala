package org.scalahttpmock.expectation

import cats.effect.IO
import org.http4s.util.CaseInsensitiveString
import org.http4s.{Header, Request}
import org.scalahttpmock.expectation.matcher._

import scala.collection.immutable.Seq

class MatchingAttempt {

  def tryMatching(expectation: ServiceExpectation, request: Request[IO]): AllMatchResult = {
    val headerMatches = tryMatchingHeaders(expectation, request)
    val urlMatch = tryMatchingUrl(expectation, request)
    val methodMatch = tryMatchingHttpMethod(expectation, request)
    val paramMatches = tryMatchingParams(expectation, request)
    val contentMatchResult = tryMatchingContent(expectation, request)

    AllMatchResult(headerMatches, methodMatch, urlMatch, paramMatches, contentMatchResult)
  }

  private def tryMatchingHeaders(expectation: ServiceExpectation,
                                 request: Request[IO]): Seq[HeaderMatchResult] = {
    expectation.headerMatchers.map { headerMatcher =>
      val default = MatchingScore(0, headerMatcher.maxScore)
      val maybeMatchingScore: MatchingScore = headerMatcher match {
        case headerEquals: HeaderEquals =>
          scoreFromMany(
            request.headers.toList,
            (header: Header) =>
              header.name == CaseInsensitiveString(headerEquals.name) && header.value == headerEquals.value,
            default
          )

        case headerMatches: HeaderMatches =>
          scoreFromMany(
            request.headers.toList,
            (header: Header) =>
              header.name == CaseInsensitiveString(headerMatches.name) && headerMatches.valueRegex
                .findFirstIn(header.value)
                .isDefined,
            default
          )
      }

      HeaderMatchResult(headerMatcher, maybeMatchingScore)
    }
  }

  private def scoreFromMany[A](many: Seq[A],
                               filter: A => Boolean,
                               defaultMatchingScore: MatchingScore): MatchingScore = {
    many
      .find(filter)
      .map(_ => defaultMatchingScore.convertToSuccess)
      .getOrElse(defaultMatchingScore)

  }

  private def tryMatchingUrl(expectation: ServiceExpectation,
                             request: Request[IO]): UriMatchResult = {

    val matchingScore: MatchingScore = expectation.uriMatcher match {
      case AnyUriMatcher => MatchingScore.success(AnyUriMatcher.maxScore)

      case pathEquals: PathEquals if pathEquals.path == request.uri.path =>
        MatchingScore.success(pathEquals.maxScore)

      case pathMatches: PathMatches
          if pathMatches.pathRegex.findFirstIn(request.uri.path).isDefined =>
        MatchingScore.success(pathMatches.maxScore)

      case uriEquals: UriEquals if uriEquals.uri == request.uri.asPathWithParams =>
        MatchingScore.success(uriEquals.maxScore)

      case uriMatches: UriMatches
          if uriMatches.uriRegex.findFirstIn(request.uri.asPathWithParams).isDefined =>
        MatchingScore.success(uriMatches.maxScore)

      case _ => MatchingScore(0, 1)

    }

    UriMatchResult(expectation.uriMatcher, matchingScore)
  }

  private def tryMatchingHttpMethod(expectation: ServiceExpectation,
                                    request: Request[IO]): HttpMethodMatchResult = {
    val matchingScore = expectation.httpMethodMatcher match {
      case AnyHttpMethodMatcher =>
        MatchingScore(AnyHttpMethodMatcher.maxScore, AnyHttpMethodMatcher.maxScore)

      case contentMatcher: HttpMethodMatcher =>
        val default = MatchingScore.fail(contentMatcher.maxScore)
        contentMatcher.maybeMethod
          .find(_ == request.method)
          .map(_ => default.convertToSuccess)
          .getOrElse(default)
    }

    HttpMethodMatchResult(expectation.httpMethodMatcher, matchingScore)
  }

  private def tryMatchingParams(expectation: ServiceExpectation,
                                request: Request[IO]): Seq[ParamMatchResult] = {

    def matchSingleParam(paramMatcher: ParamMatcher) = {
      val params = request.multiParams.map(
        //Sequences are immutable not default in this scope
        paramWithValues => (paramWithValues._1, paramWithValues._2.toList)
      )
      val matches = paramMatcher match {
        case paramMatches: ParamMatches =>
          val filter = (param: (String, Seq[String])) => {
            val (name, values) = param
            name == paramMatches.name && values.exists(
              value =>
                paramMatches.valueRegex
                  .findFirstMatchIn(value)
                  .isDefined
            )
          }

          scoreFromMany(params.toList, filter, MatchingScore.fail(paramMatches.maxScore))

        case paramEquals: ParamEquals =>
          val filter = (param: (String, Seq[String])) => {
            val (name, values) = param
            name == paramEquals.name && values.contains(paramEquals.value)

          }

          scoreFromMany(params.toList, filter, MatchingScore.fail(paramEquals.maxScore))
      }

      ParamMatchResult(paramMatcher, matches)
    }

    expectation.paramMatchers.map(matchSingleParam)
  }

  private def tryMatchingContent(expectation: ServiceExpectation,
                                 request: Request[IO]): ContentMatchResult = {

    val contentMatcher = expectation.contentMatcher
    val contentAsString = request.as[String].unsafeRunSync

    val success: MatchingScore = contentMatcher match {
      case AnyContentMatcher => MatchingScore.success(AnyContentMatcher.maxScore)

      case contentEquals: ContentEquals =>
        MatchingScore.fromMatch(contentEquals.content == contentAsString, contentEquals)

      case contentMatches: ContentMatches =>
        MatchingScore.fromMatch(contentMatches.contentRegex.findFirstIn(contentAsString).isDefined,
                                contentMatches)

      case jsonContentEquals: JsonContentEquals =>
        import io.circe.parser._
        parse(contentAsString) match {
          case Left(_) =>
            jsonContentEquals.eitherInvalidJsonOrParsedJson match {
              case Left(expectationInvalidJson) =>
                MatchingScore.fromMatch(expectationInvalidJson == contentAsString,
                                        jsonContentEquals)

              case _ => MatchingScore.fromMatch(success = false, jsonContentEquals)
            }

          case Right(parsedBodyJson) =>
            jsonContentEquals.eitherInvalidJsonOrParsedJson match {
              case Right(expectationValidJson) =>
                MatchingScore.fromMatch(expectationValidJson == parsedBodyJson, jsonContentEquals)

              case _ => MatchingScore.fromMatch(success = false, jsonContentEquals)

            }

        }
    }

    ContentMatchResult(contentMatcher, success)
  }

}
