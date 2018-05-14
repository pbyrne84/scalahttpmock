package org.scalahttpmock.expectation
import org.scalahttpmock.expectation.matcher.{HeaderMatcher, _}

import scala.collection.immutable.Seq

case class ServiceExpectation(
    headerMatchers: Seq[HeaderMatcher] = Vector(),
    httpMethodMatcher: HttpMethodMatcher = AnyHttpMethodMatcher,
    uriMatcher: UriMatcher = AnyUriMatcher,
    paramMatchers: Seq[ParamMatcher] = Vector(),
    response: MatchedResponse = JsonResponse(200, None)
) extends Indentation {

  private val headersPrettyFormat = {
    formatPrettiedCollection(convertMatchers(headerMatchers))
  }

  private def convertMatchers(matchers: Seq[PrettyText]) = {
    matchers.foldLeft(List.empty[String]) {
      case (convertedMatchers, currentMatcher) =>
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
    case hasContent: HasContent => hasContent.content
    case _ => AnyContentMatcher
  }
}
