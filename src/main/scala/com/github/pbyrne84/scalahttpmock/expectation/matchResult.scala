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
          MatchingScore(total.total + current.matchingScore.total,
                        total.possible + current.matchingScore.possible)
      }

    def totalManyScores(scores: Seq[MatchingScore]) =
      scores.foldLeft(MatchingScore.empty) {
        case (total: MatchingScore, current: MatchingScore) =>
          MatchingScore(total.total + current.total, total.possible + current.possible)
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

  val prettifyScoreWithFailedOverview: String =
    new AllMatchResultPrettifier(this).prettifyScoreWithFailedOverview

}

class AllMatchResultPrettifier private[expectation] (allMatchResult: AllMatchResult)
    extends Indentation {

  def prettifyScoreWithFailedOverview: String = {
    val failedHeaders = allMatchResult.headerMatchResults.filter(!_.success)
    val failedParams = allMatchResult.paramMatchResults.filter(!_.success)
    val invalidErrors = calculateInvalidErrors(failedHeaders, failedParams)

    if (invalidErrors.isEmpty) {
      s"[SUCCESS] SCORE:${allMatchResult.score.possible}/${allMatchResult.score.total}"
    } else {
      createInvalidMessage(failedHeaders, failedParams, invalidErrors)
    }
  }

  private def calculateInvalidErrors(failedHeaders: Seq[HeaderMatchResult],
                                     failedParams: Seq[ParamMatchResult]) = {
    Vector(
      mapError(allMatchResult.httpMethodMatchResult.success,
               s"METHOD ${allMatchResult.httpMethodMatchResult.httpMethodMatcher.prettyText}"),
      mapError(failedHeaders.isEmpty, s"HEADERS (failed ${failedHeaders.size})"),
      mapError(allMatchResult.uriMatchResult.success,
               s"URI ${allMatchResult.uriMatchResult.uriMatcher.prettyText}"),
      mapError(failedParams.isEmpty, s"PARAMS (failed ${failedParams.size})"),
      mapError(allMatchResult.contentMatchResult.success,
               s"CONTENT ${allMatchResult.contentMatchResult.contentMatcher.prettyText}")
    ).collect { case Some(error) => error }
  }

  private def mapError(isSuccess: Boolean, errorText: => String): Option[String] = {
    if (isSuccess) {
      None
    } else {
      Some(errorText)
    }
  }

  private def createInvalidMessage(failedHeaders: Seq[HeaderMatchResult],
                                   failedParams: Seq[ParamMatchResult],
                                   invalidErrors: Vector[String]): String = {
    def remapMultipleErrors(mismatches: Seq[PrettyText]) = {
      mapError(mismatches.isEmpty, s"""
         |[ ${mismatches.map(x => x.prettyText).mkString(",\n")} ]
        """.stripMargin.trim).getOrElse("None")
    }

    val nonMatchingHeaders = remapMultipleErrors(failedHeaders.map(_.headerMatcher))
    val nonMatchingParams = remapMultipleErrors(failedParams.map(_.paramMatcher))

    val indent = 27
    s"""
       |[INVALID] SCORE:${allMatchResult.score.possible}/${allMatchResult.score.total} failed {${invalidErrors
         .mkString(", ")}}
       |  Non matching headers : ${indentNewLines(indent, nonMatchingHeaders)}
       |  Non matching params  : ${indentNewLines(indent, nonMatchingParams)}
      """.stripMargin.trim
  }

}
