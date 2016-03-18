package fi.vm.sade.hakuperusteet.util

import fi.vm.sade.hakuperusteet.Urls
import org.apache.http.HttpVersion
import org.apache.http.client.fluent.Request

object HttpUtil {
  val id = "hakuperusteet"

  def urlKeyToString(key: String, args: AnyRef*) = Request.Get(Urls.urls.url(key, args:_*))
    .useExpectContinue()
    .version(HttpVersion.HTTP_1_1)
    .addHeader("clientSubSystemCode", id)
    .execute().returnContent().asString()

  def addHeaders(request: Request) = request
    .useExpectContinue()
    .version(HttpVersion.HTTP_1_1)
    .setHeader("clientSubSystemCode", id)
    .setHeader("CSRF",id)
    .setHeader("Cookie", "CSRF="+id)

  def post(key: String, args: AnyRef*) = addHeaders(Request.Post(Urls.urls.url(key, args:_*)))

}
