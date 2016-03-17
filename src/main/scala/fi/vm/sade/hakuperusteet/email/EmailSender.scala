package fi.vm.sade.hakuperusteet.email

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import fi.vm.sade.hakuperusteet.domain.{Payment, AbstractUser}
import fi.vm.sade.hakuperusteet.util.{Translate, CasClientUtils}
import fi.vm.sade.utils.cas.{CasAuthenticatingClient, CasClient, CasParams}
import org.http4s.Uri._
import org.http4s._
import org.http4s.client.Client

import scala.util.{Failure, Success, Try}
import scalaz.concurrent.Task

object EmailSender {
  def init(c: Config) = {
    val host = c.getString("hakuperusteet.cas.url")
    val username = c.getString("hakuperusteet.user")
    val password = c.getString("hakuperusteet.password")

    val casClient = new CasClient(host, org.http4s.client.blaze.defaultClient)
    val casParams = CasParams("/ryhmasahkoposti-service", username, password)
    val emailClient = new EmailClient(new CasAuthenticatingClient(casClient, casParams, org.http4s.client.blaze.defaultClient))
    new EmailSender(emailClient, c)
  }
}

class EmailSender(emailClient: EmailClient, c: Config) extends LazyLogging {
  implicit val formats = fi.vm.sade.hakuperusteet.formatsHenkilo

  def sendReceipt(userData: AbstractUser, payment: Payment) = {
    val p = ReceiptValues(userData.fullName, c.getString("vetuma.amount"), payment.reference)
    send(userData.email, Translate("email.receipt", userData.lang,"title"), EmailTemplate.renderReceipt(p, userData.lang))
  }


  def send(to: String, subject: String, body: String) = {
    val email = EmailMessage("no-reply@opintopolku.fi", subject, body, isHtml = true)
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
case class EmailMessage(from: String, subject: String, body: String, isHtml: Boolean)
case class EmailData(email: EmailMessage, recipient: List[EmailRecipient])

class EmailClient(client: Client) extends LazyLogging with CasClientUtils {
  implicit val formats = fi.vm.sade.hakuperusteet.formatsHenkilo

  def send(email: EmailData): Task[Response] = client.prepare(req(email))

  private def req(email: EmailData) = Request(
    method = Method.POST,
    uri = urlToUri("ryhmasahkoposti-service.email")
  ).withBody(email)(json4sEncoderOf[EmailData])
}
