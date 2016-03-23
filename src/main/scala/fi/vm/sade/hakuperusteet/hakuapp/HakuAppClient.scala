package fi.vm.sade.hakuperusteet.hakuapp

import java.util.concurrent.TimeUnit

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import fi.vm.sade.hakuperusteet.domain.PaymentUpdate
import fi.vm.sade.hakuperusteet.util.{CasClientUtils, HttpUtil}
import org.http4s.{Method, Request}
import org.http4s.client.Client
import fi.vm.sade.hakuperusteet.domain.PaymentState.PaymentState

object HakuAppClient {
  def init(c: Config) = {
    val timeout = c.getDuration("admin.synchronization.timeout", TimeUnit.MILLISECONDS)
    new HakuAppClient(timeout, HttpUtil.casClient(c,"/haku-app"))
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
