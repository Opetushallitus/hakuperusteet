package fi.vm.sade.hakuperusteet.auth

import javax.servlet.http.HttpServletRequest

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import fi.vm.sade.hakuperusteet.db.HakuperusteetDatabase
import fi.vm.sade.hakuperusteet.domain.Session
import fi.vm.sade.hakuperusteet.google.GoogleVerifier
import org.json4s.jackson.JsonMethods._
import org.scalatra.Control
import org.scalatra.servlet.RichRequest

class GoogleOAuth2Strategy(config: Config, db: HakuperusteetDatabase, googleVerifier: GoogleVerifier) extends SimpleAuth with LazyLogging with Control {
  import fi.vm.sade.hakuperusteet._
  val tokenName = "google"

  def authenticate(request: HttpServletRequest): Option[Session] = {
    val json = parse(RichRequest(request).body)
    val email = (json \ "email").extract[Option[String]]
    val token = (json \ "token").extract[Option[String]] // todo: currently real token
    val idpentityid = (json \ "idpentityid").extract[Option[String]] // todo: currently google
    (email, token, idpentityid) match {
      case (Some(emailFromRequest), Some(tokenFromRequest), Some(idpentityidFromSession)) if idpentityidFromSession == tokenName =>
        if (googleVerifier.verify(tokenFromRequest)) {
          Some(Session(emailFromRequest, tokenFromRequest, idpentityidFromSession))
        } else {
          logger.warn(s"Session verify failed for user $emailFromRequest with token $tokenFromRequest")
          halt(401)
        }
      case _ => None
    }
  }
}