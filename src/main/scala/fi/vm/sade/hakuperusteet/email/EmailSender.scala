package fi.vm.sade.hakuperusteet.email

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import fi.vm.sade.hakuperusteet.Urls
import fi.vm.sade.hakuperusteet.domain.{AbstractUser, Payment}
import fi.vm.sade.hakuperusteet.util.{CasClientUtils, HttpUtil, Translate}
import org.http4s._
import org.http4s.client.Client

import scala.util.{Failure, Success, Try}
import scalaz.concurrent.Task

object EmailSender {
  def init(c: Config) = {
    new EmailSender(new EmailClient(HttpUtil.casClient(c, "/ryhmasahkoposti-service")), c)
  }
}

class EmailSender(emailClient: EmailClient, c: Config) extends LazyLogging {
  implicit val formats = fi.vm.sade.hakuperusteet.formatsHenkilo

  def sendReceipt(userData: AbstractUser, payment: Payment) = {
    val p = ReceiptValues(userData.fullName, c.getString("vetuma.amount"), payment.reference)
    send(userData.email, Translate("email.receipt", userData.lang,"title"), EmailTemplate.renderReceipt(p, userData.lang))
  }


  def send(to: String, subject: String, body: String) = {
    val email = EmailMessage("no-reply@opintopolku.fi", subject, body, html = true)
    val recipients = List(EmailRecipient(to))
    val data = EmailData(email, recipients)
    logger.info(s"Sending email ($subject) to $to")
    Try { emailClient.send(data).run } match {
      case Success(r) if r.status.code == 200 =>
      case Success(r) => logger.error(s"Email sending to $to failed with statusCode ${r.status.code}, body ${EntityDecoder.decodeString(r).attemptRun}")
      case Failure(f) => logger.error(s"Email sending to $to throws", f)
    }
  }
}

case class EmailRecipient(email: String)
case class EmailMessage(from: String, subject: String, body: String, html: Boolean)
case class EmailData(email: EmailMessage, recipient: List[EmailRecipient])

class EmailClient(client: Client) extends LazyLogging with CasClientUtils {
  implicit val formats = fi.vm.sade.hakuperusteet.formatsHenkilo

  def send(email: EmailData): Task[Response] = client.fetch(req(email))

  private def req(email: EmailData) = Request(
    method = Method.POST,
    uri = urlToUri(Urls.urls.url("ryhmasahkoposti-service.email"))
  ).withBody(email)(json4sEncoderOf[EmailData])
}
