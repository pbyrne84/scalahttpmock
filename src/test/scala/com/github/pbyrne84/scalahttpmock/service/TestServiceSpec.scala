package com.github.pbyrne84.scalahttpmock.service

import com.github.pbyrne84.scalahttpmock.BaseSpec
import com.github.pbyrne84.scalahttpmock.expectation.matcher.{
  GetMatcher,
  HeaderEquals,
  HttpMethodMatcher
}
import com.github.pbyrne84.scalahttpmock.expectation.{
  Header,
  JsonResponse,
  LocationResponse,
  ServiceExpectation
}
import com.github.pbyrne84.scalahttpmock.service.implementations.JettyMockService
import com.github.pbyrne84.scalahttpmock.service.request.VerificationFailure
import org.scalatest.BeforeAndAfter
import sttp.client3._
import sttp.model.StatusCode

class TestServiceSpec extends BaseSpec with BeforeAndAfter {

  import scala.concurrent.ExecutionContext.Implicits.global

  private val port = 9001
  private val service = JettyMockService.createFutureVersion(port)
  service.start()
  implicit val backend: SttpBackend[Identity, Any] = HttpURLConnectionBackend()

  import sttp.client3.quick._

  before {
    service.reset()
  }

  "test service" should {
    "return 501 when nothing is found" in {

      service.addExpectation(
        ServiceExpectation()
          .addHeader(HeaderEquals("a", "avalue"))
          .withMethod(GetMatcher)
          .withUri("/test/anotherpath".asUriEquals)
          .withResponse(JsonResponse(202))
      )

      val uri = uri"http://localhost:$port/test/path"
      val request = basicRequest
        .get(uri)
        .header("a", "avalue")

      val response = request.send()

      response.code shouldBe StatusCode(501)
    }

    "equate all values in get" in {
      val uriText = s"http://localhost:$port/test/path"
      val responseJson = """{"a":"2"}"""
      val customHeader = Header("custom_header", "custom_header_value")
      service.addExpectation(
        ServiceExpectation()
          .addHeader(HeaderEquals("a", "avalue"))
          .withMethod(GetMatcher)
          .withUri("/test/path".asUriEquals)
          .withResponse(JsonResponse(202, Some(responseJson), Vector(customHeader)))
      )

      val uri = uri"$uriText"
      val request = basicRequest
        .get(uri)
        .header("a", "avalue")

      val response = request.send()

      response.code shouldBe StatusCode(202)

      response.headers shouldHaveEntry ("Content-Type", "application/json")
      response.headers shouldHaveEntry customHeader

      response.body shouldBe Right(responseJson)
    }

    "equate all values in post" in {
      val uriText = s"http://localhost:$port/test/path"
      val responseJson = """{"a":"2"}"""
      val payloadJson = """{"b":"4"}"""
      val customHeader = Header("custom_header", "custom_header_value")
      service.addExpectation(
        ServiceExpectation()
          .addHeader(HeaderEquals("a", "avalue"))
          .withMethod(payloadJson.asJsonPostMatcher)
          .withUri("/test/path".asUriEquals)
          .withResponse(JsonResponse(202, Some(responseJson), Vector(customHeader)))
      )

      val uri = uri"$uriText"
      val request = basicRequest
        .post(uri)
        .header("a", "avalue")
        .body(payloadJson)

      val response = request.send()

      response.code shouldBe StatusCode(202)

      response.headers shouldHaveEntry ("Content-Type", "application/json")
      response.headers shouldHaveEntry customHeader

      response.body shouldBe Right(responseJson)
    }

    "location header is returned" in {
      val redirect = "http://test.com"
      service.addExpectation(
        ServiceExpectation()
          .withUri("/test/path2".asUriEquals)
          .withMethod(HttpMethodMatcher.getMatcher)
          .withResponse(LocationResponse(303, redirect))
      )

      val uriText = s"http://localhost:$port/test/path2"

      val uri = uri"$uriText"
      val request = basicRequest
        .followRedirects(false)
        .get(uri)

      val response = request.send()

      response.code shouldBe StatusCode(303)
      response.headers shouldHaveEntry Header.location(redirect)

    }
  }

  "prioritise highest score when more than 1 match is equal" in {
    val matchAllExpectation = ServiceExpectation(response = JsonResponse(203))
    val expectedJsonResponse = """{"a" : "4"}"""
    val matchUrlExpectation = ServiceExpectation(uriMatcher = "/test/uri".asUriEquals,
                                                 response =
                                                   JsonResponse(200, Some(expectedJsonResponse)))

    service.addExpectation(matchAllExpectation)
    service.addExpectation(matchUrlExpectation)
    service.addExpectation(matchAllExpectation)

    val uri = uri"http://localhost:$port/test/uri"

    val request = basicRequest
      .post(uri)

    val response = request.send()

    response.code shouldBe StatusCode(200)
    response.body shouldBe Right(expectedJsonResponse)
  }

  "allow multiple params with the same name to be matched" in {
    val expectedJsonResponse = """{"a" : "4"}"""
    val matchUrlExpectation =
      ServiceExpectation(uriMatcher = "/test/path".asPathEquals,
                         paramMatchers = Vector(("a", "1"), ("a", "2")).asParamEquals,
                         response = JsonResponse(200, Some(expectedJsonResponse)))

    service.addExpectation(matchUrlExpectation)
    val uri = uri"http://localhost:$port/test/path?a=1&a=2"

    val request = basicRequest.get(uri)
    val response = request.send()
    response.code shouldBe StatusCode(200)
  }

  "not fail on valid verification" in {
    val expectedJsonResponse = """{"a" : "4"}"""
    val matchUrlExpectation =
      ServiceExpectation(uriMatcher = "/test/path".asPathEquals,
                         paramMatchers = Vector(("a", "1"), ("a", "2")).asParamEquals,
                         response = JsonResponse(200, Some(expectedJsonResponse)))

    service.addExpectation(matchUrlExpectation)
    val uri = uri"http://localhost:$port/test/path?a=1&a=2"

    val request = basicRequest.get(uri)
    val response = request.send()

    response.code shouldBe StatusCode(200)

    service.verifyCall(matchUrlExpectation.withUri("/test/path?a=1&a=2".asUriEquals))
  }

  "fail on invalid verification" in {
    val expectedJsonResponse = """{"a" : "4"}"""
    val matchUrlExpectation =
      ServiceExpectation(uriMatcher = "/test/path".asPathEquals,
                         paramMatchers = Vector(("a", "1"), ("a", "2")).asParamEquals,
                         response = JsonResponse(200, Some(expectedJsonResponse)))

    service.addExpectation(matchUrlExpectation)
    val uri = uri"http://localhost:$port/test/path?a=1&a=2"

    val request = basicRequest.get(uri)
    val response = request.send()

    response.code shouldBe StatusCode(200)

    a[VerificationFailure] should be thrownBy service.verifyCall(
      matchUrlExpectation.withUri("/invalid/path?a=1&a=2".asUriEquals)
    )
  }

}
