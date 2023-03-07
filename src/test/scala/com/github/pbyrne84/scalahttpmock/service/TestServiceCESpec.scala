package com.github.pbyrne84.scalahttpmock.service

import cats.effect._
import cats.effect.testing.scalatest.AsyncIOSpec
import com.github.pbyrne84.scalahttpmock.demo.ioexecutors.CENettyMockServiceExecutor
import com.github.pbyrne84.scalahttpmock.expectation.matcher.HeaderEquals
import com.github.pbyrne84.scalahttpmock.expectation.{Header, JsonResponse, ServiceExpectation}
import com.github.pbyrne84.scalahttpmock.service.executor.RunningMockServerWithOperations
import com.github.pbyrne84.scalahttpmock.service.implementations.NettyMockServer
import com.github.pbyrne84.scalahttpmock.service.request.{FreePort, VerificationFailure}
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import sttp.client3._
import sttp.client3.httpclient.cats.HttpClientCatsBackend
import sttp.model.StatusCode

class TestServiceCESpec extends AsyncFreeSpec with AsyncIOSpec with Matchers {

  import com.github.pbyrne84.scalahttpmock.testextensions.TestStringOps._
  import com.github.pbyrne84.scalahttpmock.testextensions.TestTupleOps._

  protected val expectation: TestServiceExpectations = new TestServiceExpectations

  implicit val ceOMockServiceExecutor: CENettyMockServiceExecutor = new CENettyMockServiceExecutor()

  private lazy val port = FreePort.calculate
  private lazy val service: NettyMockServer[IO] = NettyMockServer.createIoMonadVersion(port)
  // We will have to do for comprehensions for other things

  import sttp.client3.quick._

  private val dummyResponse = Response[String]("", StatusCode.apply(999))

  private def callHttpRequest(
      runningServer: RunningMockServerWithOperations[IO],
      request: RequestT[Identity, Either[String, String], Any],
      autoShutDownMockServer: Boolean = true
  ): IO[Response[String]] = {
    HttpClientCatsBackend
      .resource[IO]()
      .use { backend =>
        for {
          response <- request.response(asStringAlways).send(backend)
          _ <-
            if (autoShutDownMockServer) // sometimes we want to send multiple requests
              runningServer.shutDown
            else
              IO.pure(Right(()))
        } yield response

      }
      .handleErrorWith(handleHttpErrorAndShutdownRunningMock(runningServer))
  }

  private def handleHttpErrorAndShutdownRunningMock(
      runningServer: RunningMockServerWithOperations[IO]
  )(error: Throwable): IO[Response[String]] = {
    for {
      _ <- runningServer.shutDown
      _ <- IO.raiseError(error)
    } yield dummyResponse // we are failing so it won't get this but we need it for signatures sake
  }

  "Cats effect based test service should " - {
    "return 501 when nothing is found" in {
      val uri = uri"http://localhost:$port/test/path"
      val request = basicRequest
        .get(uri)
        .header(expectation.setupHeader._1, expectation.setupHeader._2)

      (for {
        runningServer <- service.start
        response <- callHttpRequest(runningServer, request)
      } yield response)
        .asserting(_.code shouldBe StatusCode(501))
    }

    "equate all values in get" in {
      val uriText = s"http://localhost:$port/test/path"
      val responseJson = """{"a":"2"}"""
      val customResponseHeader = Header("custom_header", "custom_header_value")

      val uri = uri"$uriText"
      val request = basicRequest
        .get(uri)
        .header(expectation.setupHeader._1, expectation.setupHeader._2)

      (for {
        runningServer <- service.start
        _ <- runningServer.addExpectation(expectation.matchingGet.implementation(responseJson, customResponseHeader))
        response <- callHttpRequest(runningServer, request)
      } yield response)
        .asserting { response =>
          response.code shouldBe StatusCode(202)
          response.headers shouldHaveEntry ("Content-Type", "application/json")
          response.headers shouldHaveEntry customResponseHeader
          response.body shouldBe responseJson
        }
    }

    "equate all values in post" in {
      val uriText = s"http://localhost:$port/test/path"
      val responseJson = """{"a":"2"}"""
      val payloadJson = """{"b":"4"}"""
      val customHeader = Header("custom_header", "custom_header_value")

      val uri = uri"$uriText"
      val request = basicRequest
        .post(uri)
        .header(expectation.setupHeader._1, expectation.setupHeader._2)
        .body(payloadJson)

      (for {
        runningServer <- service.start
        _ <- runningServer.addExpectation(
          expectation.matchingPost.implementation(payloadJson.asJsonPostMatcher, responseJson, customHeader)
        )
        response <- callHttpRequest(runningServer, request)
      } yield response)
        .asserting { response =>
          response.code shouldBe StatusCode(202)
          response.headers shouldHaveEntry ("Content-Type", "application/json")
          response.headers shouldHaveEntry customHeader
          response.body shouldBe responseJson
        }
    }

    "location header is returned" in {
      val redirectUrl = "http://test.com"
      val calledUrl = "/test/path2"

      val uriText = s"http://localhost:$port/test/path2"
      val uri = uri"$uriText"
      val request = basicRequest
        .followRedirects(false)
        .get(uri)

      (for {
        runningServer <- service.start
        _ <- runningServer.addExpectation(
          expectation.matchingGetLocationRedirect.implementation(calledUrl, redirectUrl)
        )
        response <- callHttpRequest(runningServer, request)
      } yield response)
        .asserting { response =>
          response.code shouldBe StatusCode(303)
          response.headers shouldHaveEntry Header.location(redirectUrl)
        }
    }

    "prioritise highest score when more than 1 match is equal" in {
      val matchAllExpectation = ServiceExpectation().withResponse(JsonResponse(203))
      val expectedJsonResponse = """{"a" : "4"}"""
      val matchUrlExpectation = ServiceExpectation(uriMatcher = "/test/uri".asUriEquals)
        .withResponse(JsonResponse(200, Some(expectedJsonResponse)))

      val uri = uri"http://localhost:$port/test/uri"

      val request = basicRequest
        .post(uri)

      (for {
        runningServer <- service.start
        _ <- runningServer.addExpectation(matchAllExpectation)
        _ <- runningServer.addExpectation(matchUrlExpectation)
        _ <- runningServer.addExpectation(matchAllExpectation)
        response <- callHttpRequest(runningServer, request)
      } yield response)
        .asserting { response =>
          response.code shouldBe StatusCode(200)
          response.body shouldBe expectedJsonResponse
        }

    }

    "allow multiple params with the same name to be matched" in {
      val expectedJsonResponse = """{"a" : "4"}"""
      val matchUrlExpectation =
        ServiceExpectation(
          uriMatcher = "/test/path".asPathEquals,
          paramMatchers = Vector(("a", "1"), ("a", "2")).asParamEquals
        ).withResponse(response = JsonResponse(200, Some(expectedJsonResponse)))

      val uri = uri"http://localhost:$port/test/path?a=1&a=2"

      val request = basicRequest.get(uri)

      (for {
        runningServer <- service.start
        _ <- runningServer.addExpectation(matchUrlExpectation)
        response <- callHttpRequest(runningServer, request)
      } yield response)
        .asserting { response =>
          response.code shouldBe StatusCode(200)
        }
    }

    "not fail on valid verification" in {
      val expectedJsonResponse = """{"a" : "4"}"""
      val matchUrlExpectation =
        ServiceExpectation(
          uriMatcher = "/test/path".asPathEquals,
          paramMatchers = Vector(("a", "1"), ("a", "2")).asParamEquals
        ).withResponse(response = JsonResponse(200, Some(expectedJsonResponse)))

      val uri = uri"http://localhost:$port/test/path?a=1&a=2"
      val request = basicRequest.get(uri)

      (for {
        runningServer <- service.start
        _ <- runningServer.addExpectation(matchUrlExpectation)
        response <- callHttpRequest(runningServer, request)
        _ <- runningServer.verifyCall(matchUrlExpectation.withUri("/test/path?a=1&a=2".asUriEquals))
      } yield response)
        .asserting { response =>
          response.code shouldBe StatusCode(200)
        }
    }

    "fail on invalid verification" in {
      val expectedJsonResponse = """{"a" : "4"}"""
      val matchUrlExpectation =
        ServiceExpectation(
          uriMatcher = "/test/path".asPathEquals,
          paramMatchers = Vector(("a", "1"), ("a", "2")).asParamEquals
        ).withResponse(response = JsonResponse(200, Some(expectedJsonResponse)))

      val uri = uri"http://localhost:$port/test/path?a=1&a=2"
      val request = basicRequest.get(uri)

      (for {
        runningServer <- service.start
        _ <- runningServer.addExpectation(matchUrlExpectation)
        response <- callHttpRequest(runningServer, request)
        verificationResult <- runningServer
          .verifyCall(matchUrlExpectation.withUri("/invalid/path?a=1&a=2".asUriEquals))
          .map(Right.apply)
          .recover(e => Left(e))
      } yield (response, verificationResult))
        .asserting { case (response, verificationResult) =>
          response.code shouldBe StatusCode(200)
          // Either values fails incoherently if it gets a right, similar to OptionValues
          verificationResult.left.map(_.getClass) shouldBe Left(classOf[VerificationFailure])
        }
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

      val uri = uri"$uriText"
      val request = basicRequest
        .post(uri)
        .header("a", "avalue")
        .body(payloadJson)

      (for {
        runningServer <- service.start
        _ <- runningServer.addExpectation(expectation)
        response1 <- callHttpRequest(runningServer, request, autoShutDownMockServer = false)
        response2 <- callHttpRequest(runningServer, request, autoShutDownMockServer = false)
        response3 <- callHttpRequest(runningServer, request, autoShutDownMockServer = false)
        response4 <- callHttpRequest(runningServer, request)
      } yield (response1, response2, response3, response4))
        .asserting { case (response1, response2, response3, response4) =>
          response1.code shouldBe StatusCode(202)
          response2.code shouldBe StatusCode(201)
          response3.code shouldBe StatusCode(200)
          response4.code shouldBe StatusCode(200)

          response1.headers shouldHaveEntry ("Content-Type", "application/json")
          response1.headers shouldHaveEntry customHeader
          response1.body shouldBe responseJson

        }

    }
  }

}
