package com.github.pbyrne84.scalahttpmock.expectation
import cats.data.NonEmptyList
import com.github.pbyrne84.scalahttpmock.expectation.matcher._

object ServiceExpectation {
  private val defaultResponses = NonEmptyList(JsonResponse(200, None), List.empty)
}

case class ServiceExpectation(
    headerMatchers: Seq[HeaderMatcher] = Vector(),
    httpMethodMatcher: HttpMethodMatcher = AnyHttpMethodMatcher,
    uriMatcher: UriMatcher = AnyUriMatcher,
    paramMatchers: Seq[ParamMatcher] = Vector(),
    responses: NonEmptyList[MatchedResponse] = ServiceExpectation.defaultResponses
) extends Indentation {

  private val headersPrettyFormat = {
    formatPrettiedCollection(convertMatchers(headerMatchers))
  }

  private def convertMatchers(matchers: Seq[PrettyText]) = {
    matchers.foldLeft(List.empty[String]) { case (convertedMatchers, currentMatcher) =>
      convertedMatchers :+ currentMatcher.prettyText
    }
  }

  private def formatPrettiedCollection(entries: Seq[String]) = {
    if (entries.isEmpty) {
      "Any"
    } else {
      s"""
         |[ ${entries.mkString(",\n").stripSuffix(",\n").trim} ]
      """.stripMargin.trim
    }
  }

  private val uriPrettyFormat = uriMatcher.prettyText

  private val paramsPrettyFormat = {
    formatPrettiedCollection(convertMatchers(paramMatchers))
  }

  private val methodPrettyFormat = httpMethodMatcher.prettyText

  private val bodyPrettyFormat = httpMethodMatcher.bodyPrettyText.replace("\n", "\n  ")

  val prettyFormat: String =
    s"""
      |${getClass.getSimpleName}[method="$methodPrettyFormat"](
      |  Headers : ${indentNewLines(14, headersPrettyFormat)},
      |  Uri     : $uriPrettyFormat,
      |  Params  : ${indentNewLines(14, paramsPrettyFormat)},
      |  Body    : $bodyPrettyFormat
      |)
    """.stripMargin.trim

  def contentMatcher: ContentMatcher = httpMethodMatcher match {
    case hasContent: HasContentHttpMethodMatcher =>
      hasContent.content
    case _ =>
      AnyContentMatcher
  }

  def addHeader(headerMatcher: HeaderMatcher): ServiceExpectation = copy(
    headerMatchers = headerMatchers :+ headerMatcher
  )

  def addHeaders(newHeaderMatchers: Seq[HeaderMatcher]): ServiceExpectation = copy(
    headerMatchers = headerMatchers ++ newHeaderMatchers
  )

  def withHeaders(newHeaderMatchers: Seq[HeaderMatcher]): ServiceExpectation = copy(
    headerMatchers = newHeaderMatchers
  )

  def withMethod(newHttpMethodMatcher: HttpMethodMatcher): ServiceExpectation = copy(
    httpMethodMatcher = newHttpMethodMatcher
  )

  def withUri(newUriMatcher: UriMatcher): ServiceExpectation = copy(
    uriMatcher = newUriMatcher
  )

  def addParam(paramMatcher: ParamMatcher): ServiceExpectation = copy(
    paramMatchers = paramMatchers :+ paramMatcher
  )

  def addParams(newParamMatchers: Seq[ParamMatcher]): ServiceExpectation = copy(
    paramMatchers = paramMatchers ++ newParamMatchers
  )

  def withParams(newParamMatchers: Seq[ParamMatcher]): ServiceExpectation = copy(
    paramMatchers = newParamMatchers
  )

  def withResponse(response: MatchedResponse): ServiceExpectation = copy(
    responses = NonEmptyList(response, List.empty)
  )

  def withResponses(head: MatchedResponse, tail: MatchedResponse*): ServiceExpectation = copy(
    responses = NonEmptyList(head, tail.toList)
  )

  def addResponse(response: MatchedResponse): ServiceExpectation = copy(
    responses = responses :+ response
  )

  def trimHeadResponseIfMorePending: ServiceExpectation = {
    responses.tail match {
      // this sort of stuff makes me very happy for some reason, empty list is not a nice thing to dance around
      case ::(head, next) =>
        copy(responses = NonEmptyList(head, next))
      case Nil =>
        this
    }
  }

}
