package fi.vm.sade.hakuperusteet.oppijantunnistus

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import fi.vm.sade.hakuperusteet.Urls
import fi.vm.sade.hakuperusteet.util.HttpUtil.id
import org.http4s
import org.http4s.{Method, ParseFailure, Uri}
import org.json4s.jackson.JsonMethods._
import org.json4s._
import org.json4s.JsonDSL._
import org.http4s.client.Client
import fi.vm.sade.hakuperusteet.util.{CallerIdMiddleware, CasClientUtils}
import fi.vm.sade.utils.cas.{CasParams, CasService, CasUser}
import org.json4s.jackson.Serialization.write

import scala.util.{Success, Try}

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
  private def callOppijanTunnistus(url: String, body: String): String = {
    val req = client.prepare(
      http4s.Request(
        method = Method.POST,
        uri = urlToUri(url))
        .withBody[String](body))

    Try { req.run } match {
      case Success(r) if r.status.code == 200 =>
        r.as[String].unsafePerformSync
      case _ =>
        throw new RuntimeException(s"Failed to call OppijanTunnistus: $url")
    }
  }

  def createToken(email: String, hakukohdeOid: String, uiLang: String) = {
    val siteUrlBase = if (hakukohdeOid.length > 0) s"${c.getString("host.url.base")}ao/$hakukohdeOid/#/token/" else s"${c.getString("host.url.base")}#/token/"
    val data: Map[String, String] = Map("email" -> email, "url" -> siteUrlBase, "lang" -> uiLang)

    logger.error(s"Calling create token CAS URL: ${Urls.urls.url("oppijan-tunnistus.create")}")
    callOppijanTunnistus(Urls.urls.url("oppijan-tunnistus.create"), write(compact(render(data))))
  }

  def sendToken(hakukohdeOid: String, email: String, subject: String, template: String, lang: String, expires: Long): Try[Unit] = {
    val callbackUrl = s"${c.getString("host.url.base")}#/token/"
    val data = ("email" -> email) ~
      ("url" -> callbackUrl) ~
      ("lang" -> lang) ~
      ("subject" -> subject) ~
      ("expires" -> expires) ~
      ("template" -> template)

    logger.error(s"Calling send token CAS URL: ${Urls.urls.url("oppijan-tunnistus.create")}")
    Try(callOppijanTunnistus(Urls.urls.url("oppijan-tunnistus.create"), write(compact(render(data)))))
  }

  def validateToken(token: String): Option[(String, String, Option[HakuAppMetadata])] = {
    logger.info(s"Validating token $token")

    logger.error(s"Validating token with CAS URL: ${Urls.urls.url("oppijan-tunnistus.verify")}")
    val verificationWithCapitalCaseEmail =
      parse(callOppijanTunnistus(Urls.urls.url("oppijan-tunnistus.verify", token), "")).extract[OppijanTunnistusVerification]
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
