package fi.vm.sade.hakuperusteet.util

import java.net.URL

import fi.vm.sade.hakuperusteet.Urls

object HttpUtil {
  def urlKeyToString(key: String, args: AnyRef*) = urlToString(Urls.urls.url(key, args:_*))
  private def urlToString(url: String) = io.Source.fromInputStream(new URL(url).openStream()).mkString
}
