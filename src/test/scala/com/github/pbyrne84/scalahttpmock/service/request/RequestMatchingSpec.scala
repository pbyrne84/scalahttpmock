package com.github.pbyrne84.scalahttpmock.service.request
import com.github.pbyrne84.scalahttpmock.BaseSpec
import com.github.pbyrne84.scalahttpmock.expectation.matcher.HttpMethodMatcher
import com.github.pbyrne84.scalahttpmock.expectation.{
  JsonResponse,
  MatchingAttempt,
  Method,
  ServiceExpectation
}
import org.scalatest.BeforeAndAfter

class RequestMatchingSpec extends BaseSpec with BeforeAndAfter {

  import org.mockito.Mockito._

  private val matchingAttempt: MatchingAttempt = mock(classOf[MatchingAttempt])
  private val requestMatching = new RequestMatching(matchingAttempt)

  before {
    requestMatching.reset()
  }

  "resolve response" should {

    "return empty response with empty matches when no expectations are set up" in {
      requestMatching.resolveResponse(createRequest) shouldBe PotentialResponse(
        None,
        Vector(),
        Vector()
      )
    }

    "not match when there is a single expectation that does not have a score that equals to a match" in {
      val expectation = ServiceExpectation()
      requestMatching.addExpectation(expectation)

      val request = createRequest
      val unsuccessfulMatchResult = createAnyAllMatchResult

      when(matchingAttempt.tryMatching(expectation, request))
        .thenReturn(unsuccessfulMatchResult)

      requestMatching.resolveResponse(request) shouldBe PotentialResponse(
        None,
        Vector(),
        Vector(unsuccessfulMatchResult)
      )
    }

    "match a response when total score is above 0 and equals potential score" in {
      val nonMatchingExpectation = ServiceExpectation()
      val matchingExpectation = ServiceExpectation(httpMethodMatcher = HttpMethodMatcher.getMatcher)

      requestMatching.addExpectations(List(nonMatchingExpectation, matchingExpectation))

      val request = createRequest
      val unsuccessfulMatchResult = createAnyAllMatchResult
      val successfulMatchResult = createSuccessfulMatchResult(10)

      when(matchingAttempt.tryMatching(nonMatchingExpectation, request))
        .thenReturn(unsuccessfulMatchResult)

      when(matchingAttempt.tryMatching(matchingExpectation, request))
        .thenReturn(successfulMatchResult)

      requestMatching.resolveResponse(request) shouldBe PotentialResponse(
        Some(matchingExpectation.response),
        List(successfulMatchResult),
        List(unsuccessfulMatchResult, successfulMatchResult)
      )
    }

    "return the response of the match with the highest score when there are multiple matches, multiple successful matches are also  returned" in {
      val nonMatchingExpectation = ServiceExpectation()
      val matchingExpectation1 =
        ServiceExpectation(httpMethodMatcher = HttpMethodMatcher.getMatcher)

      val matchingExpectation2 =
        ServiceExpectation(httpMethodMatcher = HttpMethodMatcher.postMatcher,
                           response = JsonResponse(200, Some("{}")))

      requestMatching.addExpectations(
        List(
          nonMatchingExpectation,
          matchingExpectation1,
          matchingExpectation2
        )
      )

      val request = createRequest
      val unsuccessfulMatchResult = createAnyAllMatchResult
      val successfulMatchResultWithLowerScore = createSuccessfulMatchResult(10)
      val successfulMatchResultWithHigherScore = createSuccessfulMatchResult(20)

      when(matchingAttempt.tryMatching(nonMatchingExpectation, request))
        .thenReturn(unsuccessfulMatchResult)

      when(matchingAttempt.tryMatching(matchingExpectation1, request))
        .thenReturn(successfulMatchResultWithLowerScore)

      when(matchingAttempt.tryMatching(matchingExpectation2, request))
        .thenReturn(successfulMatchResultWithHigherScore)

      requestMatching.resolveResponse(request) shouldBe PotentialResponse(
        maybeResponse = Some(matchingExpectation2.response),
        successfulMatches =
          List(successfulMatchResultWithHigherScore, successfulMatchResultWithLowerScore),
        allAttempts = List(unsuccessfulMatchResult,
                           successfulMatchResultWithLowerScore,
                           successfulMatchResultWithHigherScore)
      )
    }
  }

  "verifications" should {

    "raise an failure when there are no calls and no expectations" in {
      a[VerificationFailure] should be thrownBy requestMatching.verifyCall(ServiceExpectation())
    }

    "raise an error when there are calls and none match" in {
      val request1 = createRequest
      requestMatching.resolveResponse(request1)
      val request2 = createRequest
      requestMatching.resolveResponse(request2)

      val verifyExpectation = ServiceExpectation(httpMethodMatcher = HttpMethodMatcher.postMatcher)

      when(matchingAttempt.tryMatching(verifyExpectation, request1))
        .thenReturn(createAnyAllMatchResult)

      when(matchingAttempt.tryMatching(verifyExpectation, request2))
        .thenReturn(createAnyAllMatchResult)

      a[VerificationFailure] should be thrownBy requestMatching.verifyCall(
        verifyExpectation
      )
    }

    "not raise an error when there are calls and one matches" in {
      val request1 = createRequest
      requestMatching.resolveResponse(request1)

      val request2 = createRequest.copy(method = Method.POST)
      requestMatching.resolveResponse(request2)

      val verifyExpectation = ServiceExpectation(httpMethodMatcher = HttpMethodMatcher.postMatcher)

      when(matchingAttempt.tryMatching(verifyExpectation, request1))
        .thenReturn(createAnyAllMatchResult)

      when(matchingAttempt.tryMatching(verifyExpectation, request2))
        .thenReturn(createSuccessfulMatchResult(10))

      requestMatching.verifyCall(verifyExpectation)
    }

    "error when there are calls and one matches but it is supposed to match twice" in {
      val request1 = createRequest
      requestMatching.resolveResponse(request1)

      val request2 = createRequest.copy(method = Method.POST)
      requestMatching.resolveResponse(request2)

      val verifyExpectation = ServiceExpectation(httpMethodMatcher = HttpMethodMatcher.postMatcher)

      when(matchingAttempt.tryMatching(verifyExpectation, request1))
        .thenReturn(createAnyAllMatchResult)

      when(matchingAttempt.tryMatching(verifyExpectation, request2))
        .thenReturn(createSuccessfulMatchResult(10))

      a[VerificationFailure] should be thrownBy requestMatching.verifyCall(verifyExpectation, 2)
    }

    "not error when the expectation does actually match 2 requests" in {
      val request1 = createRequest
      requestMatching.resolveResponse(request1)

      val request2 = createRequest.copy(method = Method.POST)
      requestMatching.resolveResponse(request2)
      requestMatching.resolveResponse(request2)

      val verifyExpectation = ServiceExpectation(httpMethodMatcher = HttpMethodMatcher.postMatcher)

      when(matchingAttempt.tryMatching(verifyExpectation, request1))
        .thenReturn(createAnyAllMatchResult)

      when(matchingAttempt.tryMatching(verifyExpectation, request2))
        .thenReturn(createSuccessfulMatchResult(10))

      requestMatching.verifyCall(verifyExpectation, 2)
    }

  }
}
