package fi.vm.sade.hakuperusteet.hakuapp

import java.util.concurrent.TimeUnit

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import fi.vm.sade.hakuperusteet.domain.PaymentUpdate
import fi.vm.sade.hakuperusteet.util.{CasClientUtils, HttpUtil}
import org.http4s.{Method, Request}
import org.http4s.client.Client
import fi.vm.sade.hakuperusteet.domain.PaymentState.PaymentState
import org.json4s.JsonAST.JValue
import org.json4s.NoTypeHints
import org.json4s.jackson.Serialization
import org.json4s.Reader
import scalaz.{-\/, \/, \/-}
import scalaz.concurrent.Task

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

  def updateHakemusWithPaymentState(hakemusOid: String, status: PaymentState)
  = (client.toHttpService =<< updateRequest(hakemusOid, status)).map(_.orNotFound).unsafePerformSyncFor(1000L)

  def getApplicationSystemId(hakemusOid:String) = {

    implicit val formats = Serialization.formats(NoTypeHints)

    implicit val applicationReader = new Reader[Application] {
      override def read(v: JValue): Application = {
        Application((v \\ "applicationSystemId").extract[String])
      }
    }

    implicit val applicationDecored = org.http4s.json4s.native.jsonOf[Application]

    val tasks: Task[Application] = client.fetchAs(readRequest(hakemusOid))
    val response: \/[Throwable, Application] = tasks.unsafePerformSyncAttemptFor(1000l * 1l)
    response match {
      case -\/(exception) => {
        logger.error(s"Error fetching application: ${hakemusOid}", exception)
        throw exception
      }
      case \/-(application) => application.applicationSystemId
    }
  }

  private def readRequest(hakemusOid: String) = Request(
    method = Method.GET,
    uri = urlToUri(Urls.urls.url("haku-app.getApplication", hakemusOid))
  )

  private def updateRequest(hakemusOid: String, paymentStatus: PaymentState) = Request(
    method = Method.POST,
    uri = urlToUri(url(hakemusOid))
  ).withBody(PaymentUpdate(paymentStatus))(json4sEncoderOf[PaymentUpdate])
}

private case class Application(applicationSystemId: String)
