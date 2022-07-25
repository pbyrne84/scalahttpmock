package com.github.pbyrne84.scalahttpmock.expectation.matcher

import com.github.pbyrne84.scalahttpmock.expectation.{HasAnyMatchMaxScore, HasMaxScore}
import org.http4s.Method
import org.http4s.Method.{DELETE, GET, PATCH, POST, PUT}

object HttpMethodMatcher {
  val getMatcher: GetMatcher.type = GetMatcher
  val postMatcher: PostMatcher = PostMatcher()
  val putMatcher: PutMatcher = PutMatcher()
  val patchMatcher: PatchMatcher = PatchMatcher()
  val deleteMatcher: DeleteMatcher.type = DeleteMatcher

  def fromMethod(method: Method): HttpMethodMatcher = {
    method match {
      case GET => getMatcher
      case POST => postMatcher
      case PUT => putMatcher
      case PATCH => patchMatcher
      case DELETE => deleteMatcher
      case _ => throw new UnsupportedOperationException(s"$method is unsupported")
    }
  }
}

sealed trait HttpMethodMatcher extends HasMaxScore with PrettyText {
  val maybeMethod: Option[Method]

  override def maxScore: Double = 1
  val bodyPrettyText: String

}

sealed trait HasContentHttpMethodMatcher extends HttpMethodMatcher {
  val content: ContentMatcher
}

case object AnyHttpMethodMatcher extends HttpMethodMatcher with HasAnyMatchMaxScore {
  override val maybeMethod: Option[Method] = None
  override val prettyText: String = "Any"
  override val bodyPrettyText: String = "Any"
}

case object GetMatcher extends HttpMethodMatcher {
  override val maybeMethod: Option[Method] = Some(GET)
  override val prettyText: String = "Get"
  override val bodyPrettyText: String = "Ignored"
}

case object DeleteMatcher extends HttpMethodMatcher {
  override val maybeMethod = Some(DELETE)
  override val prettyText: String = "Delete"
  override val bodyPrettyText: String = "Ignored"
}

case class PostMatcher(content: ContentMatcher = AnyContentMatcher)
    extends HasContentHttpMethodMatcher {
  override val maybeMethod = Some(POST)
  override val prettyText: String = "Post"
  override val bodyPrettyText: String = content.prettyText
}

case class PutMatcher(content: ContentMatcher = AnyContentMatcher)
    extends HasContentHttpMethodMatcher {
  override val maybeMethod: Option[Method] = Some(PUT)
  override val prettyText: String = "Put"
  override val bodyPrettyText: String = content.prettyText
}

case class PatchMatcher(content: ContentMatcher = AnyContentMatcher)
    extends HasContentHttpMethodMatcher {
  override val maybeMethod: Option[Method] = Some(PATCH)
  override val prettyText: String = "Patch"
  override val bodyPrettyText: String = content.prettyText
}
