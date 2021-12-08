package com.github.pbyrne84.scalahttpmock.service

import cats.effect
import com.github.pbyrne84.scalahttpmock.expectation.{MatchedResponse, MatchingAttempt, ServiceExpectation}
import com.github.pbyrne84.scalahttpmock.service.request.{RequestMatching, ZioRequestConvertor}
import com.github.pbyrne84.scalahttpmock.service.response.ResponseRemapping
import com.typesafe.scalalogging.LazyLogging
import io.netty.buffer.Unpooled
import org.http4s
import org.http4s.EntityBody
import org.log4s.getLogger
import zhttp.http._
import zhttp.service.server.ServerChannelFactory
import zhttp.service.{EventLoopGroup, Server}
import zio._

import java.nio.charset.Charset

class ZioMockService private[ service ]( port: Int ) extends App with LazyLogging {
  private[ this ] val requestMatching      = new RequestMatching( new MatchingAttempt )
  private[ this ] val messageFailureLogger = getLogger( "org.http4s.server.message-failures" )
  private[ this ] val serviceErrorLogger   = getLogger( "org.http4s.server.service-errors" )

  private val zioRequestConvertor = new ZioRequestConvertor()

  private val app: Http[Any, Nothing, Request, UResponse] = Http.collect[Request] {
    case request: Request =>
      val convertedRequest = zioRequestConvertor.convert(request)
      val potentialResponse = requestMatching.resolveResponse(convertedRequest)

      val http4sResponse = potentialResponse.maybeResponse
        .map((response: MatchedResponse) => ResponseRemapping.respondSuccessfully(response))
        .getOrElse(
          ResponseRemapping.respondUnSuccessfully(convertedRequest, potentialResponse.allAttempts)
        )
        .unsafeRunSync()

      val body: EntityBody[effect.IO] = http4sResponse.body

      val bodyContent = body.compile.toVector.unsafeRunSync match {
        case content if content.nonEmpty =>
          HttpData.fromByteBuf(Unpooled.copiedBuffer(content.mkString, Charset.forName("UTF-8")))

        case _ =>
          HttpData.empty
      }

      val headers = http4sResponse.headers.toList.map { header: http4s.Header =>
        Header(header.name, header.value)
      }

      val response = Response.HttpResponse(
        status = zioRequestConvertor.http4StatusToZHTTPStatus( http4sResponse.status.code ),
        headers = headers,
        content = bodyContent
      )

      response
  }

  override def run( args: List[ String ] ): URIO[ zio.ZEnv, ExitCode ] = {

    val nThreads: Int = 10 //args.headOption.flatMap(x => Try(x.toInt).toOption).getOrElse(0)

    val server =
      Server.port( port ) ++ // Setup port
        Server.paranoidLeakDetection ++ // Paranoid leak detection (affects performance)
        Server.app( app )

    // Create a new server
    server.make
      .use(
        _ =>
          // Waiting for the server to start
          console.putStrLn( s"Server started on port $port" )

            // Ensures the server doesn't die after printing
            *> ZIO.never,
      )
      .provideCustomLayer( ServerChannelFactory.auto ++ EventLoopGroup.auto( nThreads ) )
      .exitCode
  }

  def reset( ): Unit = requestMatching.reset()

  def addExpectation( expectation: ServiceExpectation ): Unit =
    requestMatching.addExpectation( expectation )

  def verifyCall( expectation: ServiceExpectation, expectedTimes: Int = 1 ): Unit =
    requestMatching.verifyCall( expectation, expectedTimes )

  def shutDown( ): Unit = {} //server.shutdownNow()
}

object ZioMockServiceFactory {

  def create( port: Int ): ZioMockService = new ZioMockService( port )

  def main( args: Array[ String ] ): Unit = {
    println( new ZioMockService( 8090 ).run( List.empty )
  }

}
