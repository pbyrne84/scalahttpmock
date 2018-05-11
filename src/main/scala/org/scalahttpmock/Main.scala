package org.scalahttpmock

import cats.effect._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.server.blaze.BlazeBuilder

object Main extends App {
  private val helloWorldService = HttpService[IO] {
    case request: _root_.org.http4s.Request[_root_.cats.effect.IO] =>
      Ok(request.headers.mkString("----"))
  }

  private val builder = BlazeBuilder[IO]
    .bindHttp(8080, "localhost")
    .mountService(helloWorldService, "/")
    .start
  private val server = builder.unsafeRunSync()

  Thread.sleep(1000000000)

}
