package com.github.pbyrne84.scalahttpmock.expectation

import cats.effect.IO
import com.github.pbyrne84.scalahttpmock.expectation.matcher._
import org.eclipse.jetty.server.{Request => JettyRequest}
import org.http4s
import org.http4s.util.CaseInsensitiveString
import org.http4s.{Header, Method, Request}

import java.util.stream.Collectors
import scala.jdk.CollectionConverters.{EnumerationHasAsScala, MapHasAsScala}

case class MatchableRequest(uriPath: String,
                            asPathWithParams: String,
                            headersList: List[Header],
                            method: Method,
                            multiParams: Map[String, Seq[String]],
                            maybeContentAsString: Option[String],
                            uri: String)

object MatchableRequest {

  def fromHttpServletRequest(request: JettyRequest): MatchableRequest = {
    val headers: List[Header.Raw] = request.getHeaderNames.asScala.toList.flatMap { headerName =>
      val headersValues = request.getHeaders(headerName).asScala.toList

      headersValues.map { headerValue =>
        Header.Raw(CaseInsensitiveString(headerName), headerValue)
      }
    }

    val method = Method
      .fromString(request.getMethod)
      .getOrElse(throw new RuntimeException(s"${request.getMethod} method cannot be mapped"))

    val multiParams = request.getParameterMap.asScala.map {
      case (name, values) => name -> values.toList
    }.toMap

    val maybeContentAsString = if (request.getContentLength > 0) {
      Some(request.getReader.lines.collect(Collectors.joining(System.lineSeparator)))
    } else {
      None
    }

    val uri = request.getHttpURI.toString

    MatchableRequest(
      uriPath = request.getPathInfo,
      asPathWithParams = request.asPathWithParams,
      headersList = headers,
      method = method,
      multiParams = multiParams,
      maybeContentAsString = maybeContentAsString,
      uri = uri
    )
  }

  def fromRequestIO(request: Request[IO]): MatchableRequest = {
    val maybeContentAsString = if (request.body == http4s.EmptyBody) {
      None
    } else {
      Some(request.as[String].unsafeRunSync)
    }

    MatchableRequest(
      uriPath = request.uri.path,
      asPathWithParams = request.uri.asPathWithParams,
      headersList = request.headers.toList,
      method = request.method,
      multiParams = request.multiParams,
      maybeContentAsString = maybeContentAsString,
      uri = request.uri.toString,
    )
  }
}

class MatchingAttempt {

  def tryMatching(expectation: ServiceExpectation, request: MatchableRequest): AllMatchResult = {
    val headerMatches = tryMatchingHeaders(expectation, request)
    val urlMatch = tryMatchingUrl(expectation, request)
    val methodMatch = tryMatchingHttpMethod(expectation, request)
    val paramMatches = tryMatchingParams(expectation, request)
    val contentMatchResult = tryMatchingContent(expectation, request)

    AllMatchResult(headerMatches, methodMatch, urlMatch, paramMatches, contentMatchResult)
  }

  private def tryMatchingHeaders(expectation: ServiceExpectation,
                                 request: MatchableRequest): Seq[HeaderMatchResult] = {
    expectation.headerMatchers.map { headerMatcher =>
      val default = MatchingScore(0, headerMatcher.maxScore)
      val maybeMatchingScore: MatchingScore = headerMatcher match {
        case headerEquals: HeaderEquals =>
          scoreFromMany(
            request.headersList,
            (header: Header) =>
              header.name == CaseInsensitiveString(headerEquals.name) && header.value == headerEquals.value,
            default
          )

        case headerMatches: HeaderMatches =>
          scoreFromMany(
            request.headersList,
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
                             request: MatchableRequest): UriMatchResult = {

    val matchingScore: MatchingScore = expectation.uriMatcher match {
      case AnyUriMatcher => MatchingScore.success(AnyUriMatcher.maxScore)

      case pathEquals: PathEquals if pathEquals.path == request.uriPath =>
        MatchingScore.success(pathEquals.maxScore)

      case pathMatches: PathMatches
          if pathMatches.pathRegex.findFirstIn(request.uriPath).isDefined =>
        MatchingScore.success(pathMatches.maxScore)

      case uriEquals: UriEquals if uriEquals.uri == request.asPathWithParams =>
        MatchingScore.success(uriEquals.maxScore)

      case uriMatches: UriMatches
          if uriMatches.uriRegex.findFirstIn(request.asPathWithParams).isDefined =>
        MatchingScore.success(uriMatches.maxScore)

      case _ => MatchingScore(0, 1)

    }

    UriMatchResult(expectation.uriMatcher, matchingScore)
  }

  private def tryMatchingHttpMethod(expectation: ServiceExpectation,
                                    request: MatchableRequest): HttpMethodMatchResult = {
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
                                request: MatchableRequest): Seq[ParamMatchResult] = {

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
                                 request: MatchableRequest): ContentMatchResult = {

    val contentMatcher = expectation.contentMatcher
    val contentAsString = request.maybeContentAsString.getOrElse("")

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
