package fi.vm.sade.hakuperusteet.util

import java.nio.charset.StandardCharsets

import com.typesafe.config.Config
import fi.vm.sade.hakuperusteet.Urls
import fi.vm.sade.utils.cas.{CasAuthenticatingClient, CasClient, CasParams}
import org.apache.http.HttpVersion
import org.apache.http.client.fluent.Request
import org.http4s.client.Client

object HttpUtil {
  val id = "1.2.246.562.10.00000000001.hakuperusteet"

  def urlKeyToString(key: String, args: AnyRef*) = Request.Get(Urls.urls.url(key, args:_*))
    .useExpectContinue()
    .version(HttpVersion.HTTP_1_1)
    .addHeader("clientSubSystemCode", id)
    .addHeader("Caller-Id", id)
    .execute().returnContent().asString(StandardCharsets.UTF_8)

  def addHeaders(request: Request) = request
    .useExpectContinue()
    .version(HttpVersion.HTTP_1_1)
    .setHeader("clientSubSystemCode", id)
    .setHeader("Caller-Id", id)
    .setHeader("CSRF",id)
    .setHeader("Cookie", "CSRF="+id)

  def post(key: String, args: AnyRef*) = addHeaders(Request.Post(Urls.urls.url(key, args:_*)))

  def casClient(c: Config, service: String): Client = {
    val host = c.getString("hakuperusteet.cas.url")
    val username = c.getString("hakuperusteet.user")
    val password = c.getString("hakuperusteet.password")
    val casClient = new CasClient(host, org.http4s.client.blaze.defaultClient)
    val casParams = CasParams(service, username, password)
    CasAuthenticatingClient(casClient, casParams, org.http4s.client.blaze.defaultClient, id)
  }

}
