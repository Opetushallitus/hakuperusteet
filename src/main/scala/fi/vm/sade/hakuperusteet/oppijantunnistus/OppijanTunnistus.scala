package fi.vm.sade.hakuperusteet.oppijantunnistus

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import fi.vm.sade.hakuperusteet.util.HttpUtil
import org.apache.http.entity.ContentType
import org.json4s.jackson.JsonMethods._
import org.json4s._
import org.json4s.JsonDSL._

import scala.util.{Try, Success, Failure}

case class OppijanTunnistusVerification(email: Option[String], valid: Boolean, metadata: Option[Map[String,String]], lang: Option[String])
case class HakuAppMetadata(hakemusOid: String, personOid: String)

case class OppijanTunnistus(c: Config) extends LazyLogging {
  import fi.vm.sade.hakuperusteet._

  def parseHakuAppMetadata(metadata: Map[String, String]): Option[HakuAppMetadata] = {
    val hakemusOid = metadata.get("hakemusOid")
    val personOid = metadata.get("personOid")
    (hakemusOid, personOid) match {
      case (Some(hakemusOid), Some(personOid)) => Some(HakuAppMetadata(hakemusOid, personOid))
      case _ => None
    }
  }

  def createToken(email: String, hakukohdeOid: String, uiLang: String) = {
    val siteUrlBase = if (hakukohdeOid.length > 0) s"${c.getString("host.url.base")}ao/$hakukohdeOid/#/token/" else s"${c.getString("host.url.base")}#/token/"
    val data = Map("email" -> email, "url" -> siteUrlBase, "lang" -> uiLang)

    HttpUtil.post("oppijan-tunnistus.create")
      .bodyString(compact(render(data)), ContentType.APPLICATION_JSON)
      .execute().returnContent().asString()
  }

  def sendToken(hakukohdeOid: String, email: String, subject: String, template: String, lang: String, expires: Long): Try[Unit] = {
    val callbackUrl = s"${c.getString("host.url.base")}#/token/"
    val data = ("email" -> email) ~
      ("url" -> callbackUrl) ~
      ("lang" -> lang) ~
      ("subject" -> subject) ~
      ("expires" -> expires) ~
      ("template" -> template)
    Try(HttpUtil.post("oppijan-tunnistus.create")
      .bodyString(compact(render(data)), ContentType.APPLICATION_JSON)
      .execute().returnResponse()) match {
      case Success(r) if 200 == r.getStatusLine.getStatusCode => Success(())
      case Success(_) => Failure(new RuntimeException(s"Failed to send authentication email to $email"))
      case Failure(e) => Failure(new RuntimeException(s"Failed to send authentication email to $email", e))
    }
  }

  def validateToken(token: String): Option[(String, String, Option[HakuAppMetadata])] = {
    logger.info(s"Validating token $token")

    val verifyResult = HttpUtil.urlKeyToString("oppijan-tunnistus.verify", token)
    val verificationWithCapitalCaseEmail = parse(verifyResult).extract[OppijanTunnistusVerification]
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
  def init(c: Config) = OppijanTunnistus(c)
}
