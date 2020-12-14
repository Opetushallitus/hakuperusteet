package fi.vm.sade.hakuperusteet.oppijantunnistus

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import fi.vm.sade.hakuperusteet.Urls
import fi.vm.sade.hakuperusteet.email.{EmailData, EmailMessage, EmailRecipient}
import fi.vm.sade.hakuperusteet.util.HttpUtil.id
import org.http4s
import org.http4s.{Header, Headers, Method, ParseFailure, Request, Response, Uri}
import org.json4s.jackson.JsonMethods._
import org.json4s._
import org.json4s.JsonDSL._
import org.http4s.client.Client
import fi.vm.sade.hakuperusteet.util.{CallerIdMiddleware, CasClientUtils}
import fi.vm.sade.utils.cas.{CasParams, CasService, CasUser}
import org.json4s.jackson.Serialization.write
import scalaz.concurrent.Task

import scala.util.{Failure, Success, Try}

case class OppijanTunnistusVerification(email: Option[String], valid: Boolean, metadata: Option[Map[String,String]], lang: Option[String])
case class HakuAppMetadata(hakemusOid: String, personOid: String)

case class OppijanTunnistus(client: Client, c: Config) extends LazyLogging with CasClientUtils {
  implicit val formats = fi.vm.sade.hakuperusteet.formatsOppijanTunnistus

  def parseHakuAppMetadata(metadata: Map[String, String]): Option[HakuAppMetadata] = {
    val hakemusOid = metadata.get("hakemusOid")
    val personOid = metadata.get("personOid")
    (hakemusOid, personOid) match {
      case (Some(hakemusOid), Some(personOid)) => Some(HakuAppMetadata(hakemusOid, personOid))
      case _ => None
    }
  }

  trait OppijanTunnistusData

  case class Data(email: String, url: String, lang: String,
                  subject: Option[String] = None,
                  expires: Option[Long] = None,
                  template: Option[String] = None) extends OppijanTunnistusData

  private def callOppijanTunnistus(url: String, body: Option[Data]): String = {
    implicit val formats = fi.vm.sade.hakuperusteet.formatsHenkilo

    var r: Task[Request] = body match {
      case Some(body) =>
        http4s.Request(
          method = Method.POST,
          uri = urlToUri(url)).withBody(body)(json4sEncoderOf[Data])
      case None =>
        http4s.Request(
          method = Method.POST,
          uri = urlToUri(url)).withBody(null)
    }

    val req = client.prepare(r)

    Try { req.run } match {
      case Success(r) if r.status.code == 200 =>
        r.as[String].unsafePerformSync
      case Success(r) if r.status.code != 200 =>
        logger.error(s"Failed to call url $url: Status ${r.status.code}")
        throw new RuntimeException(s"Failed to call OppijanTunnistus: $url")
      case Failure(e) =>
        logger.error(s"Failed to call url $url: $e")
        throw new RuntimeException(s"Failed to call OppijanTunnistus: $url")
    }
  }

  def createToken(email: String, hakukohdeOid: String, uiLang: String) = {
    val siteUrlBase = if (hakukohdeOid.length > 0) s"${c.getString("host.url.base")}ao/$hakukohdeOid/#/token/" else s"${c.getString("host.url.base")}#/token/"
    val data: Map[String, String] = Map("email" -> email, "url" -> siteUrlBase, "lang" -> uiLang)

    logger.error(s"Calling create token CAS URL: ${Urls.urls.url("oppijan-tunnistus.create")}")
    callOppijanTunnistus(Urls.urls.url("oppijan-tunnistus.create"),
      Some(Data(email=email, url = siteUrlBase, lang =uiLang)))
  }

  def sendToken(hakukohdeOid: String, email: String, subject: String, template: String, lang: String, expires: Long): Try[Unit] = {
    val callbackUrl = s"${c.getString("host.url.base")}#/token/"
    logger.error(s"Calling send token CAS URL: ${Urls.urls.url("oppijan-tunnistus.create")}")
    Try(callOppijanTunnistus(Urls.urls.url("oppijan-tunnistus.create"),
      Some(Data(email = email, url=callbackUrl, lang=lang, subject=Some(subject), expires=Some(expires),template=Some(template))
    )))
  }

  def validateToken(token: String): Option[(String, String, Option[HakuAppMetadata])] = {
    logger.info(s"Validating token $token")

    logger.error(s"Validating token with CAS URL: ${Urls.urls.url("oppijan-tunnistus.verify")}")
    val verificationWithCapitalCaseEmail =
      parse(callOppijanTunnistus(Urls.urls.url("oppijan-tunnistus.verify", token), None)).extract[OppijanTunnistusVerification]
    val verification = verificationWithCapitalCaseEmail.copy(email = verificationWithCapitalCaseEmail.email.map(_.toLowerCase))
    if(verification.valid) {
      verification.email match {
        case Some(email) => Some(email, verification.lang.get, parseHakuAppMetadata(verification.metadata.getOrElse(Map())))
        case _ => None
      }
    } else {
      None
    }

  }
}

object OppijanTunnistus {
  def init(c: Config) = {
    Uri.fromString("/oppijan-tunnistus/auth/cas").fold(
      (e: ParseFailure) => throw new IllegalArgumentException(e),
      (service: Uri) => {
        val host = c.getString("hakuperusteet.cas.url")
        val username = c.getString("hakuperusteet.user")
        val password = c.getString("hakuperusteet.password")

        val casClient = new RingCasClient(host, CallerIdMiddleware(org.http4s.client.blaze.defaultClient))
        val casParams = CasParams(CasService(service), CasUser(username, password))

        OppijanTunnistus(
          RingCasAuthenticatingClient(casClient, casParams, CallerIdMiddleware(org.http4s.client.blaze.defaultClient), id), c)
      })
  }
}
