package com.github.pbyrne84.scalahttpmock.service

import com.github.pbyrne84.scalahttpmock.expectation.matcher.HeaderEquals
import com.github.pbyrne84.scalahttpmock.expectation.{Header, JsonResponse, ServiceExpectation}
import com.github.pbyrne84.scalahttpmock.service.executor.RunningMockServerWithOperations
import com.github.pbyrne84.scalahttpmock.service.implementations.NettyMockServer
import com.github.pbyrne84.scalahttpmock.service.request.{FreePort, VerificationFailure}
import com.typesafe.scalalogging.StrictLogging
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import sttp.client3._
import sttp.model.StatusCode

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

//Futures with eager running are always fun
class TestServiceFutureSpec
    extends TestServiceBaseSpec
    with BeforeAndAfter
    with BeforeAndAfterAll
    with StrictLogging
    with ScalaFutures {

  import com.github.pbyrne84.scalahttpmock.testextensions.TestStringOps._
  import com.github.pbyrne84.scalahttpmock.testextensions.TestTupleOps._

  import scala.concurrent.ExecutionContext.Implicits.global

  // private lazy val port = FreePort.calculate
  private lazy val port = FreePort.calculate
  // futures eagerly running are so fun
  private lazy val service = NettyMockServer.createFutureVersion(port)
  private val serverWaitDuration: Duration = Duration.apply(20, TimeUnit.SECONDS)
  private lazy val runningService: RunningMockServerWithOperations[Future] = {
    try {
      Await.result(service.start, serverWaitDuration)
    } catch {
      case e: Throwable =>
        throw new RuntimeException(s"Could not start future test on port $port", e)
    }
  }
  private val backend: SttpBackend[Identity, Any] = HttpURLConnectionBackend()

  import sttp.client3.quick._

  before {
    runningService.reset
  }

  override def afterAll(): Unit = {
    // This will actually cause the worst error as failures in after or before are really hard to debug
    // Usually I would just let the mock engine auto shut after all the test runs as what can happen is tests
    // start relying on other tests to start things up so just starting fake services once globally
    // is a bit more child proof. I am a child so I am including myself in that statement :)
    Await.result(runningService.shutDown, serverWaitDuration).left.map(e => throw e)
  }

  "future based test service" should {
    "return 501 when nothing is found" in {
      runningService
        .addExpectation(
          expectation.nonMatching.implementation
        )
        .futureValue

      val uri = uri"http://localhost:$port/test/path"
      val request = basicRequest
        .get(uri)
        .header(expectation.setupHeader._1, expectation.setupHeader._2)

      val response = request.send(backend)

      response.code shouldBe StatusCode(501)
    }

    "equate all values in get" in {
      val uriText = s"http://localhost:$port/test/path"
      val responseJson = """{"a":"2"}"""
      val customResponseHeader = Header("custom_header", "custom_header_value")

      runningService
        .addExpectation(
          expectation.matchingGet.implementation(responseJson, customResponseHeader)
        )
        .futureValue

      val uri = uri"$uriText"
      val request = basicRequest
        .get(uri)
        .header(expectation.setupHeader._1, expectation.setupHeader._2)

      val response = request.send(backend)

      response.code shouldBe StatusCode(202)

      response.headers shouldHaveEntry ("Content-Type", "application/json")
      response.headers shouldHaveEntry customResponseHeader

      response.body shouldBe Right(responseJson)
    }

    "equate all values in post" in {
      val uriText = s"http://localhost:$port/test/path"
      val responseJson = """{"a":"2"}"""
      val payloadJson = """{"b":"4"}"""
      val customHeader = Header("custom_header", "custom_header_value")
      runningService
        .addExpectation(
          expectation.matchingPost.implementation(payloadJson.asJsonPostMatcher, responseJson, customHeader)
        )
        .futureValue

      val uri = uri"$uriText"
      val request = basicRequest
        .post(uri)
        .header(expectation.setupHeader._1, expectation.setupHeader._2)
        .body(payloadJson)

      val response = request.send(backend)

      response.code shouldBe StatusCode(202)

      response.headers shouldHaveEntry ("Content-Type", "application/json")
      response.headers shouldHaveEntry customHeader

      response.body shouldBe Right(responseJson)
    }

    "location header is returned" in {
      val redirectUrl = "http://test.com"
      val calledUrl = "/test/path2"

      runningService
        .addExpectation(
          expectation.matchingGetLocationRedirect.implementation(calledUrl, redirectUrl)
        )
        .futureValue

      val uriText = s"http://localhost:$port/test/path2"
      val uri = uri"$uriText"
      val request = basicRequest
        .followRedirects(false)
        .get(uri)

      val response = request.send(backend)

      response.code shouldBe StatusCode(303)
      response.headers shouldHaveEntry Header.location(redirectUrl)

    }

    "prioritise highest score when more than 1 match is equal" in {
      val matchAllExpectation = ServiceExpectation().withResponse(JsonResponse(203))
      val expectedJsonResponse = """{"a" : "4"}"""
      val matchUrlExpectation = ServiceExpectation(uriMatcher = "/test/uri".asUriEquals)
        .withResponse(JsonResponse(200, Some(expectedJsonResponse)))

      runningService.addExpectation(matchAllExpectation).futureValue
      runningService.addExpectation(matchUrlExpectation).futureValue
      runningService.addExpectation(matchAllExpectation).futureValue

      val uri = uri"http://localhost:$port/test/uri"

      val request = basicRequest
        .post(uri)

      val response = request.send(backend)

      response.code shouldBe StatusCode(200)
      response.body shouldBe Right(expectedJsonResponse)
    }

    "allow multiple params with the same name to be matched" in {
      val expectedJsonResponse = """{"a" : "4"}"""
      val matchUrlExpectation =
        ServiceExpectation(
          uriMatcher = "/test/path".asPathEquals,
          paramMatchers = Vector(("a", "1"), ("a", "2")).asParamEquals
        ).withResponse(response = JsonResponse(200, Some(expectedJsonResponse)))

      runningService.addExpectation(matchUrlExpectation).futureValue
      val uri = uri"http://localhost:$port/test/path?a=1&a=2"

      val request = basicRequest.get(uri)
      val response = request.send(backend)
      response.code shouldBe StatusCode(200)
    }

    "not fail on valid verification" in {
      val expectedJsonResponse = """{"a" : "4"}"""
      val matchUrlExpectation =
        ServiceExpectation(
          uriMatcher = "/test/path".asPathEquals,
          paramMatchers = Vector(("a", "1"), ("a", "2")).asParamEquals
        ).withResponse(response = JsonResponse(200, Some(expectedJsonResponse)))

      runningService.addExpectation(matchUrlExpectation).futureValue
      val uri = uri"http://localhost:$port/test/path?a=1&a=2"

      val request = basicRequest.get(uri)
      val response = request.send(backend)

      response.code shouldBe StatusCode(200)

      runningService.verifyCall(matchUrlExpectation.withUri("/test/path?a=1&a=2".asUriEquals)).futureValue
    }

    "fail on invalid verification" in {
      val expectedJsonResponse = """{"a" : "4"}"""
      val matchUrlExpectation =
        ServiceExpectation(
          uriMatcher = "/test/path".asPathEquals,
          paramMatchers = Vector(("a", "1"), ("a", "2")).asParamEquals
        ).withResponse(response = JsonResponse(200, Some(expectedJsonResponse)))

      runningService.addExpectation(matchUrlExpectation).futureValue
      val uri = uri"http://localhost:$port/test/path?a=1&a=2"

      val request = basicRequest.get(uri)
      val response = request.send(backend)

      response.code shouldBe StatusCode(200)

      runningService
        .verifyCall(
          matchUrlExpectation.withUri("/invalid/path?a=1&a=2".asUriEquals)
        )
        .failed
        .futureValue shouldBe a[VerificationFailure]

    }

    "allow chaining of responses repeating the last one on further calls" in {
      val uriText = s"http://localhost:$port/test/path"
      val responseJson = """{"a":"2"}"""
      val payloadJson = """{"b":"4"}"""
      val customHeader = Header("custom_header", "custom_header_value")
      val expectation = ServiceExpectation()
        .addHeader(HeaderEquals("a", "avalue"))
        .withMethod(payloadJson.asJsonPostMatcher)
        .withUri("/test/path".asUriEquals)
        .withResponses(
          JsonResponse(202, Some(responseJson), Vector(customHeader)),
          JsonResponse(201, Some(responseJson), Vector(customHeader)),
          JsonResponse(200, Some(responseJson), Vector(customHeader))
        )

      runningService.addExpectation(expectation).futureValue

      val uri = uri"$uriText"
      val request = basicRequest
        .post(uri)
        .header("a", "avalue")
        .body(payloadJson)

      val response = request.send(backend)

      response.code shouldBe StatusCode(202)

      response.headers shouldHaveEntry ("Content-Type", "application/json")
      response.headers shouldHaveEntry customHeader

      response.body shouldBe Right(responseJson)

      request.send(backend).code shouldBe StatusCode(201)
      request.send(backend).code shouldBe StatusCode(200)
      request.send(backend).code shouldBe StatusCode(200)

    }
  }

}
