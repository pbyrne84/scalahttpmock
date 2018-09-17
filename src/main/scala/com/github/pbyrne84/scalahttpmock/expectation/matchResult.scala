package com.github.pbyrne84.scalahttpmock.expectation
import com.github.pbyrne84.scalahttpmock.expectation.matcher._

import scala.collection.immutable.Seq

sealed trait SingleMatchResult {
  val success: Boolean
}

trait HasMaxScore {
  def maxScore: Double
}

trait HasAnyMatchMaxScore extends HasMaxScore {
  override def maxScore: Double = 0.5
}

trait HasScore {
  def matchingScore: MatchingScore
}

case class HeaderMatchResult(headerMatcher: HeaderMatcher, matchingScore: MatchingScore)
    extends SingleMatchResult
    with HasScore {
  override val success: Boolean = matchingScore.isMatch
}

case class UriMatchResult(uriMatcher: UriMatcher, matchingScore: MatchingScore)
    extends SingleMatchResult
    with HasScore {
  override val success: Boolean = matchingScore.isMatch
}

case class HttpMethodMatchResult(httpMethodMatcher: HttpMethodMatcher, matchingScore: MatchingScore)
    extends SingleMatchResult
    with HasScore {
  override val success: Boolean = matchingScore.isMatch
}

case class ParamMatchResult(paramMatcher: ParamMatcher, matchingScore: MatchingScore)
    extends SingleMatchResult
    with HasScore {
  override val success: Boolean = matchingScore.isMatch
}

case class ContentMatchResult(contentMatcher: ContentMatcher, matchingScore: MatchingScore)
    extends SingleMatchResult
    with HasScore {
  override val success: Boolean = matchingScore.isMatch
}

object MatchingScore {

  def empty: MatchingScore = new MatchingScore(0, 0)

  def fromMatch(success: Boolean, hasMaxScore: HasMaxScore): MatchingScore =
    if (success) {
      MatchingScore.success(hasMaxScore.maxScore)
    } else {
      MatchingScore.fail(hasMaxScore.maxScore)
    }

  def success(possible: Double) = MatchingScore.apply(possible, possible)
  def fail(possible: Double) = MatchingScore.apply(0, possible)

  def apply(total: Double, possible: Double) =
    new MatchingScore(total, possible)
}

case class MatchingScore private[expectation] (total: Double, possible: Double) {
  def +(otherMatchingScore: MatchingScore): MatchingScore = {
    MatchingScore(total + otherMatchingScore.total, possible + otherMatchingScore.possible)
  }

  val isMatch: Boolean = {
    if (total > 0) {
      possible == total
    } else {
      false
    }
  }

  val percentage: Double = {
    if (total == 0 || possible == 0) {
      0
    } else {
      total / possible * 100
    }
  }

  def convertToSuccess: MatchingScore = MatchingScore(possible, possible)
}

case class AllMatchResult(headerMatchResults: Seq[HeaderMatchResult],
                          httpMethodMatchResult: HttpMethodMatchResult,
                          uriMatchResult: UriMatchResult,
                          paramMatchResults: Seq[ParamMatchResult],
                          contentMatchResult: ContentMatchResult)
    extends Indentation {

  val score: MatchingScore = {
    def totalManyHasScores(scores: Seq[HasScore]) =
      scores.foldLeft(MatchingScore.empty) {
        case (total: MatchingScore, current: HasScore) =>
          total + current.matchingScore
      }

    def totalManyScores(scores: Seq[MatchingScore]): MatchingScore = {
      scores.foldLeft(MatchingScore.empty) {
        case (total: MatchingScore, current: MatchingScore) =>
          total + current
      }
    }

    val headerTotal = totalManyHasScores(headerMatchResults)
    val paramTotal = totalManyHasScores(paramMatchResults)

    totalManyScores(
      Vector(headerTotal,
             httpMethodMatchResult.matchingScore,
             uriMatchResult.matchingScore,
             paramTotal,
             contentMatchResult.matchingScore)
    )
  }
  val matches: Boolean =
    score.isMatch

  private val prettifier = new AllMatchResultPrettifier(this)

  lazy val prettifyResult: String = prettifier.prettifyResult

  lazy val nonMatchingHeaders: Seq[HeaderMatchResult] = headerMatchResults.filter(!_.success)
  lazy val matchingHeaders: Seq[HeaderMatchResult] = headerMatchResults.filter(_.success)
  lazy val nonMatchingParams: Seq[ParamMatchResult] = paramMatchResults.filter(!_.success)
  lazy val matchingParams: Seq[ParamMatchResult] = paramMatchResults.filter(_.success)

}

class AllMatchResultPrettifier private[expectation] (allMatchResult: AllMatchResult)
    extends Indentation {

  def prettifyResult: String = {
    val invalidErrors = calculateInvalidErrors

    if (invalidErrors.isEmpty) {
      s"[SUCCESS] SCORE:${allMatchResult.score.possible}/${allMatchResult.score.total}"
    } else {
      createFailedRequestMessage(invalidErrors)
    }
  }

  private def calculateInvalidErrors: Vector[String] = {
    val failedHeaders: Seq[HeaderMatchResult] = allMatchResult.nonMatchingHeaders
    val failedParams: Seq[ParamMatchResult] = allMatchResult.nonMatchingParams

    Vector(
      mapError(allMatchResult.httpMethodMatchResult.success,
               s"METHOD ${allMatchResult.httpMethodMatchResult.httpMethodMatcher.prettyText}"),
      mapError(failedHeaders.isEmpty, s"HEADERS (failed ${failedHeaders.size})"),
      mapError(allMatchResult.uriMatchResult.success,
               s"URI ${allMatchResult.uriMatchResult.uriMatcher.prettyText}"),
      mapError(failedParams.isEmpty, s"PARAMS (failed ${failedParams.size})"),
      mapError(allMatchResult.contentMatchResult.success,
               s"CONTENT ${allMatchResult.contentMatchResult.contentMatcher.shortDescription}")
    ).collect { case Some(error) => error }
  }

  private def mapError(isSuccess: Boolean, errorText: => String): Option[String] = {
    if (isSuccess) {
      None
    } else {
      Some(errorText)
    }
  }

  private def createFailedRequestMessage(invalidErrors: Vector[String]): String = {
    def remapMultiple(mismatches: Seq[PrettyText]) = {
      mapError(mismatches.isEmpty, s"""
                                      |[ ${mismatches.map(x => x.prettyText).mkString(",\n")} ]
        """.stripMargin.trim).getOrElse("None")
    }

    val successfulMatchingHeadersText = remapMultiple(
      allMatchResult.matchingHeaders.map(_.headerMatcher)
    )

    val nonMatchingHeadersText = remapMultiple(
      allMatchResult.nonMatchingHeaders.map(_.headerMatcher)
    )
    val successfulMatchingParamsText = remapMultiple(
      allMatchResult.matchingParams.map(_.paramMatcher)
    )
    val nonMatchingParamsText = remapMultiple(allMatchResult.nonMatchingParams.map(_.paramMatcher))

    def matchingOrNonMatching(isMAtching: Boolean) =
      if (isMAtching) {
        "Matching    "
      } else {
        "Non matching"
      }

    val methodMatchText = matchingOrNonMatching(allMatchResult.httpMethodMatchResult.success)
    val uriMatchText = matchingOrNonMatching(allMatchResult.uriMatchResult.success)
    val contentMatchText = matchingOrNonMatching(allMatchResult.contentMatchResult.success)

    val indent = 29
    s"""
       |[INVALID] SCORE:${allMatchResult.score.possible}/${allMatchResult.score.total} failed {${invalidErrors
         .mkString(", ")}}
       |  Method  - $methodMatchText : ${allMatchResult.httpMethodMatchResult.httpMethodMatcher.prettyText}
       |  Headers - Matching     : ${indentNewLines(indent, successfulMatchingHeadersText)}
       |  Headers - Non matching : ${indentNewLines(indent, nonMatchingHeadersText)}
       |  Uri     - $uriMatchText : ${allMatchResult.uriMatchResult.uriMatcher.prettyText}
       |  Params  - Matching     : ${indentNewLines(indent, successfulMatchingParamsText)}
       |  Params  - Non matching : ${indentNewLines(indent, nonMatchingParamsText)}
       |  Content - $contentMatchText : ${indentNewLines(2, {
         allMatchResult.contentMatchResult.contentMatcher.prettyText
       })}
      """.stripMargin.trim
  }
}
