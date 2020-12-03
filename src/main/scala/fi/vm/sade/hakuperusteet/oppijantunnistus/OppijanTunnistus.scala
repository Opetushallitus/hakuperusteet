package fi.vm.sade.hakuperusteet.oppijantunnistus

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import fi.vm.sade.hakuperusteet.Urls
import org.http4s
import org.http4s.{Method}
import org.json4s.jackson.JsonMethods._
import org.json4s._
import org.json4s.JsonDSL._
import org.http4s.client.Client
import fi.vm.sade.hakuperusteet.util.{CasClientUtils, HttpUtil}
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

    Try(callOppijanTunnistus(Urls.urls.url("oppijan-tunnistus.create"), write(compact(render(data)))))
  }

  def validateToken(token: String): Option[(String, String, Option[HakuAppMetadata])] = {
    logger.info(s"Validating token $token")

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
  def init(c: Config) = OppijanTunnistus(HttpUtil.casClient(c, "/oppijant-tunnistus/auth/cas"), c)
}
