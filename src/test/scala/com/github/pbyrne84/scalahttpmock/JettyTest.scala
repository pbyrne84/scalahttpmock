//package com.github.pbyrne84.scalahttpmock
//
//import org.eclipse.jetty.server.Server
//import org.eclipse.jetty.servlet.DefaultServlet
//import org.eclipse.jetty.util.resource.Resource
//import org.eclipse.jetty.webapp.WebAppContext
//
////import org.mockito.Mockito.{verify, verifyNoMoreInteractions, when }
////import org.scalatest.mock.MockitoSugar
////import org.scalatest.{Assertions, BeforeAndAfter, FunSuite}
////import scala.collection.JavaConversions._
////// with BeforeAndAfter with MockitoSugar with Assertions
//object JettyTest {
//
//  def main(args: Array[String]): Unit = {
//    val port = System.getProperty("port", "8080").toInt
//    val server = new Server(port)
//
//    val context = new WebAppContext()
//    context.setContextPath("/")
//    context.setResourceBase(
//      Resource.newResource(ClassLoader.getSystemResource("public")).getURI.toASCIIString
//    )
//
//    context.addServlet(classOf[DefaultServlet], "/")
//
//    server.setHandler(context)
//
//    server.start()
//    server.join()
//  }
//}
