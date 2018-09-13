package com.github.pbyrne84.scalahttpmock

import java.io.File

import org.clapper.classutil.{ClassFinder, ClassInfo}
import org.http4s.Status

import scala.collection.immutable

object GenerateStatusIntWithGenerator extends App {

  import scala.reflect.runtime.{universe => ru}

  case class Person(name: String)

  val m = ru.runtimeMirror(getClass.getClassLoader)

  val classPerson = ru.typeOf[Person].typeSymbol.asClass

  private val http4sDslJar: File = ??? // use location of jar on file system

  val finder = ClassFinder(http4sDslJar :: Nil)

  val classes: immutable.Seq[ClassInfo] =
    finder.getClasses().filter(_.implements("org.http4s.dsl.impl.ResponseGenerator")).toList

  val allCalulations = classes.map { classInfo =>
    classInfo.toString -> calculateStatusCodedForEntityProvider(classInfo)
  }.toMap

  println(allCalulations)

  private def calculateStatusCodedForEntityProvider(classInfo: ClassInfo) = {
    val childClasses = finder
      .getClasses()
      .filter(_.implements(classInfo.toString))
      .filter(_.toString.endsWith("Ops"))
      .toList

    val ints = childClasses
      .map(
        clazz => lookupUpSingleStatus(convertOpsToStatusMethod(clazz.toString)).code
      )
      .sorted
    ints
  }

  private def convertOpsToStatusMethod(opsClassName: String) = {
    try {
      opsClassName.substring(opsClassName.indexOf("$") + 1, opsClassName.indexOf("Ops"))
    } catch {
      case e: Exception => throw new RuntimeException(s"failed converting $opsClassName")
    }
  }

  private def lookupUpSingleStatus(fieldName: String): Status = {
    val statusField = Status.getClass.getDeclaredField(fieldName)
    statusField.setAccessible(true)
    statusField.get(Status).asInstanceOf[Status]
  }

}
