package com.github.pbyrne84.scalahttpmock.service.request

import com.typesafe.scalalogging.StrictLogging

import java.net.ServerSocket
import scala.util.{Failure, Success, Using}

object FreePort extends StrictLogging {

  def calculate: Int = {
    // Need to remember to close the damn port :)
    Using(new ServerSocket(0)) { socket =>
      socket.getLocalPort
    } match {
      case Failure(exception) => throw exception
      case Success(calculatedPort) =>
        logger.info(s"Service will use $calculatedPort")
        calculatedPort
    }
  }
}
