package com.github.pbyrne84.scalahttpmock.expectation.matcher

import com.github.pbyrne84.scalahttpmock.expectation.MockHttpMethod._
import com.github.pbyrne84.scalahttpmock.expectation.{HasAnyMatchMaxScore, HasMaxScore, MockHttpMethod}

object HttpMethodMatcher {
  val getMatcher: GetMatcher.type = GetMatcher
  val postMatcher: PostMatcher = PostMatcher()
  val putMatcher: PutMatcher = PutMatcher()
  val patchMatcher: PatchMatcher = PatchMatcher()
  val deleteMatcher: DeleteMatcher.type = DeleteMatcher

  def fromMethod(method: MockHttpMethod): HttpMethodMatcher = {
    method match {
      case GET => getMatcher
      case POST => postMatcher
      case PUT => putMatcher
      case PATCH => patchMatcher
      case DELETE => deleteMatcher
    }
  }
}

sealed trait HttpMethodMatcher extends HasMaxScore with PrettyText {
  val maybeMethod: Option[MockHttpMethod]

  override def maxScore: Double = 1
  val bodyPrettyText: String

}

sealed trait HasContentHttpMethodMatcher extends HttpMethodMatcher {
  val content: ContentMatcher
}

case object AnyHttpMethodMatcher extends HttpMethodMatcher with HasAnyMatchMaxScore {
  override val maybeMethod: Option[MockHttpMethod] = None
  override val prettyText: String = "Any"
  override val bodyPrettyText: String = "Any"
}

case object GetMatcher extends HttpMethodMatcher {
  override val maybeMethod: Option[MockHttpMethod] = Some(GET)
  override val prettyText: String = "Get"
  override val bodyPrettyText: String = "Ignored"
}

case object DeleteMatcher extends HttpMethodMatcher {
  override val maybeMethod: Option[MockHttpMethod] = Some(DELETE)
  override val prettyText: String = "Delete"
  override val bodyPrettyText: String = "Ignored"
}

case class PostMatcher(content: ContentMatcher = AnyContentMatcher) extends HasContentHttpMethodMatcher {
  override val maybeMethod: Option[MockHttpMethod] = Some(POST)
  override val prettyText: String = "Post"
  override val bodyPrettyText: String = content.prettyText
}

case class PutMatcher(content: ContentMatcher = AnyContentMatcher) extends HasContentHttpMethodMatcher {
  override val maybeMethod: Option[MockHttpMethod] = Some(PUT)
  override val prettyText: String = "Put"
  override val bodyPrettyText: String = content.prettyText
}

case class PatchMatcher(content: ContentMatcher = AnyContentMatcher) extends HasContentHttpMethodMatcher {
  override val maybeMethod: Option[MockHttpMethod] = Some(PATCH)
  override val prettyText: String = "Patch"
  override val bodyPrettyText: String = content.prettyText
}
