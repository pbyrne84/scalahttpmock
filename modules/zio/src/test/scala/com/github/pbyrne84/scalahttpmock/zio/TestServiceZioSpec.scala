package com.github.pbyrne84.scalahttpmock.zio

import com.github.pbyrne84.scalahttpmock.expectation.matcher.HeaderEquals
import com.github.pbyrne84.scalahttpmock.expectation.{Header, JsonResponse, ServiceExpectation}
import com.github.pbyrne84.scalahttpmock.service.TestServiceExpectations
import com.github.pbyrne84.scalahttpmock.service.request.VerificationFailure
import sttp.client3.httpclient.zio.HttpClientZioBackend
import sttp.model.StatusCode
import zio.ZIO
import zio.test.Assertion.isSubtype
import zio.test.TestAspect.sequential
import zio.test._
object TestServiceZioSpec extends ZIOBaseSpec {

  import com.github.pbyrne84.scalahttpmock.testextensions.TestTupleOps._
  import sttp.client3._ // all the basicRequest/asStringAlways stuff etc.

  private val expectation: TestServiceExpectations = new TestServiceExpectations

  import com.github.pbyrne84.scalahttpmock.testextensions.TestStringOps._

  private def callHttpRequest(
      request: RequestT[Identity, Either[String, String], Any]
  ): ZIO[Any, Throwable, Response[String]] = {
    for {
      backend <- HttpClientZioBackend()
      response <- backend
        .send(
          request
            .response(asStringAlways)
        )
    } yield response
  }

  override def spec = {
    suite(getClass.getSimpleName)(
      suite("zio based test service should")(
        test("return 501 when nothing is found") {
          val uri = uri"http://localhost:$port/test/path"
          val request = basicRequest
            .get(uri)
            .header(expectation.setupHeader._1, expectation.setupHeader._2)

          for {
            _ <- ZioNettyMockServer.reset
            _ <- ZioNettyMockServer.addExpectation(expectation.nonMatching.implementation)
            response <- callHttpRequest(request)
          } yield assertTrue(response.code == StatusCode(501))
        },
        test("equate all values in get") {
          val uriText = s"http://localhost:$port/test/path"
          val responseJson = """{"a":"2"}"""
          val customResponseHeader = Header("custom_header", "custom_header_value")
          val uri = uri"$uriText"
          val request = basicRequest
            .get(uri)
            .header(expectation.setupHeader._1, expectation.setupHeader._2)

          for {
            _ <- ZioNettyMockServer.reset
            _ <- ZioNettyMockServer.addExpectation(
              expectation.matchingGet.implementation(responseJson, customResponseHeader)
            )
            response <- callHttpRequest(request)
          } yield assertTrue(
            response.code == StatusCode(202),
            response.headers.tupledContains("Content-Type" -> "application/json"),
            response.headers.tupledContains(customResponseHeader.name.value -> customResponseHeader.value)
          )
        },
        test("equate all values in post") {
          val uriText = s"http://localhost:$port/test/path"
          val responseJson = """{"a":"2"}"""
          val payloadJson = """{"b":"4"}"""
          val customHeader = Header("custom_header", "custom_header_value")

          val uri = uri"$uriText"
          val request = basicRequest
            .post(uri)
            .header(expectation.setupHeader._1, expectation.setupHeader._2)
            .body(payloadJson)

          for {
            _ <- ZioNettyMockServer.reset
            _ <- ZioNettyMockServer.addExpectation(
              expectation.matchingPost.implementation(payloadJson.asJsonPostMatcher, responseJson, customHeader)
            )
            response <- callHttpRequest(request)
          } yield assertTrue(
            response.code == StatusCode(202),
            response.code == StatusCode(202),
            response.headers.tupledContains("Content-Type" -> "application/json"),
            response.headers.tupledContains(customHeader.name.value -> customHeader.value)
          )
        },
        test("location header is returned") {
          val redirectUrl = "http://test.com"
          val calledUrl = "/test/path2"

          val uriText = s"http://localhost:$port/test/path2"
          val uri = uri"$uriText"
          val request = basicRequest
            .followRedirects(false)
            .get(uri)

          for {
            _ <- ZioNettyMockServer.reset
            _ <- ZioNettyMockServer.addExpectation(
              expectation.matchingGetLocationRedirect.implementation(calledUrl, redirectUrl)
            )
            response <- callHttpRequest(request)
          } yield {
            val locationHeader = Header.location(redirectUrl)
            assertTrue(
              response.code == StatusCode(303),
              response.headers.tupledContains(
                locationHeader.name.value -> locationHeader.value
              )
            )
          }
        },
        test("prioritise highest score when more than 1 match is equal") {
          val matchAllExpectation = ServiceExpectation().withResponse(JsonResponse(203))
          val expectedJsonResponse = """{"a" : "4"}"""
          val matchUrlExpectation = ServiceExpectation(uriMatcher = "/test/uri".asUriEquals)
            .withResponse(JsonResponse(200, Some(expectedJsonResponse)))

          val uri = uri"http://localhost:$port/test/uri"

          val request = basicRequest
            .post(uri)

          for {
            _ <- ZioNettyMockServer.reset
            _ <- ZioNettyMockServer.addExpectation(matchAllExpectation)
            _ <- ZioNettyMockServer.addExpectation(matchUrlExpectation)
            _ <- ZioNettyMockServer.addExpectation(matchAllExpectation)
            response <- callHttpRequest(request)

          } yield assertTrue(response.code == StatusCode(200), response.body == expectedJsonResponse)
        },
        test("allow multiple params with the same name to be matched") {
          val expectedJsonResponse = """{"a" : "4"}"""
          val matchUrlExpectation =
            ServiceExpectation(
              uriMatcher = "/test/path".asPathEquals,
              paramMatchers = Vector(("a", "1"), ("a", "2")).asParamEquals
            ).withResponse(response = JsonResponse(200, Some(expectedJsonResponse)))

          val uri = uri"http://localhost:$port/test/path?a=1&a=2"

          for {
            _ <- ZioNettyMockServer.reset
            _ <- ZioNettyMockServer.addExpectation(matchUrlExpectation)
            response <- callHttpRequest(basicRequest.get(uri))
          } yield assertTrue(response.code == StatusCode(200))
        },
        test("not fail on valid verification") {
          val expectedJsonResponse = """{"a" : "4"}"""
          val matchUrlExpectation =
            ServiceExpectation(
              uriMatcher = "/test/path".asPathEquals,
              paramMatchers = Vector(("a", "1"), ("a", "2")).asParamEquals
            ).withResponse(response = JsonResponse(200, Some(expectedJsonResponse)))

          val uri = uri"http://localhost:$port/test/path?a=1&a=2"

          for {
            _ <- ZioNettyMockServer.reset
            _ <- ZioNettyMockServer.addExpectation(matchUrlExpectation)
            response <- callHttpRequest(basicRequest.get(uri))
            _ <- ZioNettyMockServer.verifyCall(matchUrlExpectation.withUri("/test/path?a=1&a=2".asUriEquals))
          } yield assertTrue(response.code == StatusCode(200))

        },
        test("fail on invalid verification") {
          val expectedJsonResponse = """{"a" : "4"}"""
          val matchUrlExpectation =
            ServiceExpectation(
              uriMatcher = "/test/path".asPathEquals,
              paramMatchers = Vector(("a", "1"), ("a", "2")).asParamEquals
            ).withResponse(response = JsonResponse(200, Some(expectedJsonResponse)))

          val uri = uri"http://localhost:$port/test/path?a=1&a=2"

          for {
            _ <- ZioNettyMockServer.reset
            _ <- ZioNettyMockServer.addExpectation(matchUrlExpectation)
            response <- callHttpRequest(basicRequest.get(uri))
            verifyError <- ZioNettyMockServer
              .verifyCall(matchUrlExpectation.withUri("/invalid/path?a=1&a=2".asUriEquals))
              .flip
          } yield assertTrue(
            response.code == StatusCode(200)
          ) && assert(verifyError)(isSubtype[VerificationFailure](Assertion.anything))
        },
        test("allow chaining of responses repeating the last one on further calls") {
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

          val uri = uri"$uriText"
          val request = basicRequest
            .post(uri)
            .header("a", "avalue")
            .body(payloadJson)

          for {
            _ <- ZioNettyMockServer.reset
            _ <- ZioNettyMockServer.addExpectation(expectation)
            firstResponse <- callHttpRequest(request)
            secondResponse <- callHttpRequest(request)
            thirdResponse <- callHttpRequest(request)
            fourthResponse <- callHttpRequest(request)
          } yield assertTrue(
            firstResponse.code == StatusCode(202),
            firstResponse.headers.tupledContains("Content-Type", "application/json"),
            firstResponse.headers.tupledContains(customHeader.name.value -> customHeader.value),
            firstResponse.body == responseJson,
            secondResponse.code == StatusCode(201),
            thirdResponse.code == StatusCode(200),
            fourthResponse.code == StatusCode(200)
          )
        },
        test("shutdown at end") {
          // really like to handle this via a scoped layer but tomorrows problem.
          ZioNettyMockServer.shutDown.map(result => assertTrue(result == Right(())))
        }
      )
    )

  } @@ sequential
}
