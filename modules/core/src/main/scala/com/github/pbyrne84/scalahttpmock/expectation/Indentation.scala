package com.github.pbyrne84.scalahttpmock.expectation

trait Indentation {
  protected def indentNewLines(size: Int, text: String, indentFirstLine: Boolean = false): String = {
    val padding = "".padTo(size, " ").mkString
    val initialIndent = if (indentFirstLine) {
      padding
    } else {
      ""
    }
    val indentedText = initialIndent + text.replaceAll("\n", s"\n$padding")

    val trimEmtpyLinesRegex = "\n( +)\n"
    indentedText.replaceAll(trimEmtpyLinesRegex, "\n\n")

  }

}
