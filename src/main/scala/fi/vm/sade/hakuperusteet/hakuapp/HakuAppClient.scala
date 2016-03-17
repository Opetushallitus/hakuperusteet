package fi.vm.sade.hakuperusteet.hakuapp

import java.util.concurrent.TimeUnit

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import fi.vm.sade.hakuperusteet.domain.PaymentUpdate
import fi.vm.sade.hakuperusteet.util.CasClientUtils
import fi.vm.sade.utils.cas.{CasAuthenticatingClient, CasParams, CasClient}
import org.http4s.Uri._
import org.http4s.{Uri, Method, Request}
import org.http4s.client.Client
import fi.vm.sade.hakuperusteet.domain.PaymentState.PaymentState

object HakuAppClient {
  def init(c: Config) = {
    val host = c.getString("hakuperusteet.cas.url")
    val username = c.getString("hakuperusteet.user")
    val password = c.getString("hakuperusteet.password")
    val timeout = c.getDuration("admin.synchronization.timeout", TimeUnit.MILLISECONDS)
    val casClient = new CasClient(host, org.http4s.client.blaze.defaultClient)
    val casParams = CasParams("/haku-app", username, password)
    new HakuAppClient(timeout, new CasAuthenticatingClient(casClient, casParams, org.http4s.client.blaze.defaultClient))
  }
}

class HakuAppClient(timeout: Long, client: Client) extends LazyLogging with CasClientUtils {
  import fi.vm.sade.hakuperusteet._

  def url(hakemusOid: String): String = {
    Urls.urls.url("haku-app.updatePaymentStatus", hakemusOid)
  }

  def updateHakemusWithPaymentState(hakemusOid: String, status: PaymentState) = client.prepare(req(hakemusOid, status)).runFor(timeoutInMillis = timeout)

  private def req(hakemusOid: String, paymentStatus: PaymentState) = Request(
    method = Method.POST,
    uri = urlToUri(url(hakemusOid))
  ).withBody(PaymentUpdate(paymentStatus))(json4sEncoderOf[PaymentUpdate])

}
