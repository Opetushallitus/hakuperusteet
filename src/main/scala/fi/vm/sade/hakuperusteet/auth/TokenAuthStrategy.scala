package fi.vm.sade.hakuperusteet.auth

import javax.servlet.http.HttpServletRequest
import fi.vm.sade.hakuperusteet.domain.AbstractUser._
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import fi.vm.sade.hakuperusteet.db.HakuperusteetDatabase
import fi.vm.sade.hakuperusteet.domain._
import fi.vm.sade.hakuperusteet.henkilo.HenkiloClient
import fi.vm.sade.hakuperusteet.oppijantunnistus.OppijanTunnistus
import fi.vm.sade.hakuperusteet.util.PaymentUtil
import org.json4s.jackson.JsonMethods._
import org.scalatra.Control
import org.scalatra.servlet.RichRequest

import scala.util.{Failure, Success, Try}

class TokenAuthStrategy (config: Config, db: HakuperusteetDatabase, oppijanTunnistus: OppijanTunnistus) extends SimpleAuth with LazyLogging with Control {
  import fi.vm.sade.hakuperusteet._
  val henkiloClient = HenkiloClient.init(config)

  def authenticate(request: HttpServletRequest): Option[Session] = {
    val json = parse(RichRequest(request).body)
    val token = (json \ "token").extract[Option[String]]
    val idpentityid = (json \ "idpentityid").extract[Option[String]]
    (token, idpentityid) match {
      case (Some(tokenFromRequest), Some(idpentityidFromSession)) if idpentityidFromSession == OppijaToken.toString => createSession(tokenFromRequest)
      case _ => None
    }
  }

  def createSession(tokenFromRequest: String) = {
    Try { oppijanTunnistus.validateToken(tokenFromRequest) } match {
      case Success(Some((email, lang, Some(metadata)))) =>
        val partialUser: PartialUser = AbstractUser.partialUser(None, Some(metadata.personOid), email, OppijaToken, lang)
        upsertIdpEntity(partialUser)
        val existingUser = db.findUser(email)
        val user: AbstractUser = existingUser.orElse(db.upsertPartialUser(partialUser)).get
        if(existingUser.isDefined) {
          val validPayment = PaymentUtil.getValidPayment(db.findPayments(user))
          if(validPayment.isDefined) {
            db.insertPaymentSyncRequest(user, validPayment.get.copy(hakemusOid = Some(metadata.hakemusOid)))
          }
        }
        Some(Session(email, tokenFromRequest, OppijaToken.toString))
      case Success(Some((email, lang, None))) => Some(Session(email, tokenFromRequest, OppijaToken.toString))
      case Success(None) => None
      case Failure(f) =>
        logger.error("Oppijantunnistus.validateToken error", f)
        halt(401)
    }
  }
  
  def upsertIdpEntity(user: AbstractUser): Unit = {
    Try(henkiloClient.upsertIdpEntity(user).run) match {
      case Success(h) =>
      case Failure(f) =>
        logger.error("henkiloClient.upsertIdpEntity error inserting idp entity for user: " + user, f)
        halt(500)
    }
  }
}

object TokenAuthStrategy {
  import fi.vm.sade.hakuperusteet._

  def hasTokenInRequest(request: HttpServletRequest) = Try((parse(RichRequest(request).body) \ "token").extract[Option[String]].isDefined).getOrElse(false)
}
