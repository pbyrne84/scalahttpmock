package org.scalahttpmock.service.request
import cats.effect.IO
import org.http4s.Request
import org.scalahttpmock.expectation.{
  AllMatchResult,
  MatchedResponse,
  MatchingAttempt,
  ServiceExpectation
}

import scala.collection.immutable.Seq
import scala.collection.mutable.ListBuffer
class VerificationFailure private[this] (message: String) extends RuntimeException(message) {

  def this(expectation: ServiceExpectation,
           expectedTimesCalled: Int,
           actualTimesCalled: Int,
           verificationResults: Seq[RequestVerificationResult]) =
    this(
      "*************************************\n" +
        s"The following expectation as matched $actualTimesCalled out of $expectedTimesCalled times:-\n" +
        expectation.prettyFormat +
        "\nThe following requests were made:-\n" + verificationResults
        .sortBy(_.allMatchResult.score.percentage)
        .reverse
        .map(_.prettifyVerification)
        .mkString("\n\n")
        + "\n*************************************\n"
    )
}

case class PotentialResponse(maybeResponse: Option[MatchedResponse],
                             successfulMatches: Seq[AllMatchResult],
                             allAttempts: Seq[AllMatchResult])

case class RequestVerificationResult(request: Request[IO], allMatchResult: AllMatchResult) {
  val matches: Boolean = allMatchResult.matches

  import org.scalahttpmock.expectation.RequestPrettification._

  val prettifyVerification: String = allMatchResult.prettifyScoreWithFailedOverview + "\n" +
    request.prettyFormat

}

class RequestMatching(matchingAttempt: MatchingAttempt) {

  private val expectations: ListBuffer[ServiceExpectation] = ListBuffer.empty[ServiceExpectation]
  private val serviceRequests: ListBuffer[Request[IO]] = ListBuffer.empty[Request[IO]]

  case class MatchWithResponse(result: AllMatchResult, expectationResponse: MatchedResponse)

  private val root = org.slf4j.LoggerFactory
    .getLogger("ROOT")

  def addExpectation(serviceExpectation: ServiceExpectation): Unit =
    expectations += serviceExpectation

  def addExpectations(serviceExpectations: Seq[ServiceExpectation]): Unit =
    expectations ++= serviceExpectations

  def resolveResponse(request: Request[IO]): PotentialResponse = {
    serviceRequests += request

    val attempts: List[MatchWithResponse] = expectations.toList.map {
      expectation: ServiceExpectation =>
        MatchWithResponse(matchingAttempt.tryMatching(expectation, request), expectation.response)
    }

    val sortedSuccessfulMatches =
      attempts.filter(_.result.matches).sortBy(_.result.score.total).reverse

    val maybeResponse = sortedSuccessfulMatches.headOption
      .map { matchWithResult =>
        Some(matchWithResult.expectationResponse)
      }
      .getOrElse {
        root.warn(s"no expectations have been setup")
        None
      }

    PotentialResponse(maybeResponse, sortedSuccessfulMatches.map(_.result), attempts.map(_.result))
  }

  def reset(): Unit = {
    expectations.clear()
    serviceRequests.clear()
  }

  def verifyCall(expectation: ServiceExpectation, expectedTimes: Int = 1): Unit = {
    val allMatchResults: Seq[RequestVerificationResult] = serviceRequests.toList.map {
      serviceRequest =>
        RequestVerificationResult(serviceRequest,
                                  matchingAttempt.tryMatching(expectation, serviceRequest))
    }

    val actualTimes = allMatchResults.count(_.matches)
    val hasBeenCalled = actualTimes == expectedTimes
    if (!hasBeenCalled) {
      throw new VerificationFailure(expectation, expectedTimes, actualTimes, allMatchResults)
    }
  }
}
