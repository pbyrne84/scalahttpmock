package org.scalahttpmock.expectation

trait Indentation {
  protected def indentNewLines(size: Int, text: String): String = {
    val padding = "".padTo(size, " ").mkString

    text.replaceAll("\n", s"\n$padding")
  }

}
