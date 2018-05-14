package org.scalahttpmock
import org.http4s.Uri

package object expectation {

  implicit class UriAs(uri: Uri) {
    def asPathWithParams: String = {
      val protocolLength = uri.scheme.map(_.value.length + 3).getOrElse(0)
      val hostLength = uri.host.map(_.value.length).getOrElse(0)
      val portLength = uri.authority
        .flatMap { authority =>
          authority.port.collect { case port => port.toString.length + 1 }
        }
        .getOrElse(0)

      uri.renderString.substring(protocolLength + hostLength + portLength)
    }
  }
}
