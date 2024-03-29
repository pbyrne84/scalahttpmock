package com.github.pbyrne84.scalahttpmock.service.request
import com.github.pbyrne84.scalahttpmock.expectation._

import scala.collection.mutable.ListBuffer
class VerificationFailure private[this] (message: String) extends RuntimeException(message) {

  def this(
      expectation: ServiceExpectation,
      expectedTimesCalled: Int,
      actualTimesCalled: Int,
      verificationResults: Seq[RequestVerificationResult]
  ) =
    this(
      "*************************************\n" +
        s"The following expectation was matched $actualTimesCalled out of $expectedTimesCalled times:-\n" +
        expectation.prettyFormat +
        s"\nThe following requests were made (${verificationResults.length}):-\n" + verificationResults
          .sortBy(_.allMatchResult.score.percentage)
          .reverse
          .map(_.prettifyVerification)
          .mkString("\n\n")
        + "\n*************************************\n"
    )
}

case class PotentialResponse(
    maybeResponse: Option[MatchedResponse],
    successfulMatches: Seq[AllMatchResult],
    allAttempts: Seq[AllMatchResult]
)

case class RequestVerificationResult(request: MatchableRequest, allMatchResult: AllMatchResult) {
  val matches: Boolean = allMatchResult.matches

  import com.github.pbyrne84.scalahttpmock.expectation.RequestPrettification._

  val prettifyVerification: String = allMatchResult.prettifyResult + "\n" +
    request.prettyFormat

}

class RequestMatching(matchingAttempt: MatchingAttempt) {

  private val expectations: ListBuffer[ServiceExpectation] = ListBuffer.empty[ServiceExpectation]
  private val serviceRequests: ListBuffer[MatchableRequest] = ListBuffer.empty[MatchableRequest]

  case class MatchWithResponse(result: AllMatchResult, expectationResponse: MatchedResponse)

  override def toString: String = {
    expectations.toList.map((expatation: ServiceExpectation) => expatation.toString).mkString("\n")
  }

  private val root = org.slf4j.LoggerFactory
    .getLogger("ROOT")

  def addExpectation(serviceExpectation: ServiceExpectation): Unit =
    expectations += serviceExpectation

  def addExpectations(serviceExpectations: Seq[ServiceExpectation]): Unit =
    expectations ++= serviceExpectations

  def resolveResponse(request: MatchableRequest): PotentialResponse = {
    serviceRequests += request

    val attempts: Seq[(Int, MatchWithResponse)] = expectations.toList.zipWithIndex.map {
      case (expectation: ServiceExpectation, index: Int) =>
        index -> MatchWithResponse(matchingAttempt.tryMatching(expectation, request), expectation.responses.head)
    }

    val sortedSuccessfulMatches =
      attempts.filter(_._2.result.matches).sortBy(_._2.result.score.total).reverse

    val maybeResponse = sortedSuccessfulMatches.headOption
      .map { case (index, matchWithResult) =>
        // icky for now
        expectations(index) = expectations(index).trimHeadResponseIfMorePending

        Some(matchWithResult.expectationResponse)
      }
      .getOrElse {
        root.warn(s"no expectations have been setup")
        None
      }

    PotentialResponse(
      maybeResponse = maybeResponse,
      successfulMatches = sortedSuccessfulMatches.map(_._2.result),
      allAttempts = attempts
        .map(_._2.result)
    )
  }

  def reset(): Unit = {
    expectations.clear()
    serviceRequests.clear()
  }

  def verifyCall(expectation: ServiceExpectation, expectedTimes: Int = 1): Unit = {
    val allMatchResults: Seq[RequestVerificationResult] = serviceRequests.toList.map { serviceRequest =>
      RequestVerificationResult(serviceRequest, matchingAttempt.tryMatching(expectation, serviceRequest))
    }

    val actualTimes = allMatchResults.count(_.matches)
    val hasBeenCalled = actualTimes == expectedTimes
    if (!hasBeenCalled) {
      throw new VerificationFailure(expectation, expectedTimes, actualTimes, allMatchResults)
    }
  }
}
