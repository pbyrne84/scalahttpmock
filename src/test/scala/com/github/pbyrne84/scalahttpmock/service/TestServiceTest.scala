package com.github.pbyrne84.scalahttpmock.service

import com.github.pbyrne84.scalahttpmock.BaseTest
import com.github.pbyrne84.scalahttpmock.expectation.matcher.{GetMatcher, HeaderEquals}
import com.github.pbyrne84.scalahttpmock.expectation.{JsonResponse, ServiceExpectation}
import com.github.pbyrne84.scalahttpmock.service.request.VerificationFailure
import com.softwaremill.sttp._
import org.http4s.Header
import org.scalatest.BeforeAndAfter

class TestServiceTest extends BaseTest with BeforeAndAfter {

  private val port = 9001
  private val service = new TestService(port)
  implicit val backend: SttpBackend[Id, Nothing] = HttpURLConnectionBackend()

  before {
    service.reset()
  }

  "test service" should {
    "return 501 when nothing is found" in {

      service.addExpectation(
        ServiceExpectation()
          .addHeader(HeaderEquals("a", "avalue"))
          .changeMethod(GetMatcher)
          .changeUri("/test/anotherpath".asUriEquals)
          .changeResponse(JsonResponse(202))
      )

      val uri = uri"http://localhost:$port/test/path"
      val request = sttp
        .get(uri)
        .header("a", "avalue")

      val response = request.send()

      response.code shouldBe 501
    }

    "equate all values in get" in {
      val uriText = s"http://localhost:$port/test/path"
      val responseJson = """{"a":"2"}"""
      val customHeader = Header("custom_header", "custom_header_value")
      service.addExpectation(
        ServiceExpectation()
          .addHeader(HeaderEquals("a", "avalue"))
          .changeMethod(GetMatcher)
          .changeUri("/test/path".asUriEquals)
          .changeResponse(JsonResponse(202, Some(responseJson), Vector(customHeader)))
      )

      val uri = uri"$uriText"
      val request = sttp
        .get(uri)
        .header("a", "avalue")

      val response = request.send()

      response.code shouldBe 202

      response.headers shouldHaveEntry ("Content-Type", "application/json")
      response.headers shouldHaveEntry customHeader

      response.unsafeBody shouldBe responseJson
    }

    "equate all values in post" in {
      val uriText = s"http://localhost:$port/test/path"
      val responseJson = """{"a":"2"}"""
      val payloadJson = """{"b":"4"}"""
      val customHeader = Header("custom_header", "custom_header_value")
      service.addExpectation(
        ServiceExpectation()
          .addHeader(HeaderEquals("a", "avalue"))
          .changeMethod(payloadJson.asJsonPostMatcher)
          .changeUri("/test/path".asUriEquals)
          .changeResponse(JsonResponse(202, Some(responseJson), Vector(customHeader)))
      )

      val uri = uri"$uriText"
      val request = sttp
        .post(uri)
        .header("a", "avalue")
        .body(payloadJson)

      val response = request.send()

      response.code shouldBe 202

      response.headers shouldHaveEntry ("Content-Type", "application/json")
      response.headers shouldHaveEntry customHeader

      response.unsafeBody shouldBe responseJson
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

    val request = sttp
      .post(uri)

    val response = request.send()

    response.code shouldBe 200
    response.unsafeBody shouldBe expectedJsonResponse
  }

  "allow multiple params with the same name to be matched" in {
    val expectedJsonResponse = """{"a" : "4"}"""
    val matchUrlExpectation =
      ServiceExpectation(uriMatcher = "/test/path".asPathEquals,
                         paramMatchers = Vector(("a", "1"), ("a", "2")).asParamEquals,
                         response = JsonResponse(200, Some(expectedJsonResponse)))

    service.addExpectation(matchUrlExpectation)
    val uri = uri"http://localhost:$port/test/path?a=1&a=2"

    val request = sttp.get(uri)
    val response = request.send()
    response.code shouldBe 200
  }

  "not fail on valid verification" in {
    val expectedJsonResponse = """{"a" : "4"}"""
    val matchUrlExpectation =
      ServiceExpectation(uriMatcher = "/test/path".asPathEquals,
                         paramMatchers = Vector(("a", "1"), ("a", "2")).asParamEquals,
                         response = JsonResponse(200, Some(expectedJsonResponse)))

    service.addExpectation(matchUrlExpectation)
    val uri = uri"http://localhost:$port/test/path?a=1&a=2"

    val request = sttp.get(uri)
    val response = request.send()

    response.code shouldBe 200

    service.verifyCall(matchUrlExpectation.changeUri("/test/path?a=1&a=2".asUriEquals))
  }

  "fail on invalid verification" in {
    val expectedJsonResponse = """{"a" : "4"}"""
    val matchUrlExpectation =
      ServiceExpectation(uriMatcher = "/test/path".asPathEquals,
                         paramMatchers = Vector(("a", "1"), ("a", "2")).asParamEquals,
                         response = JsonResponse(200, Some(expectedJsonResponse)))

    service.addExpectation(matchUrlExpectation)
    val uri = uri"http://localhost:$port/test/path?a=1&a=2"

    val request = sttp.get(uri)
    val response = request.send()

    response.code shouldBe 200

    a[VerificationFailure] should be thrownBy service.verifyCall(
      matchUrlExpectation.changeUri("/invalid/path?a=1&a=2".asUriEquals)
    )
  }

}
