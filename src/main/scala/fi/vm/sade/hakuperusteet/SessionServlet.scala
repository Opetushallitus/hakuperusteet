package fi.vm.sade.hakuperusteet

import com.typesafe.config.Config
import fi.vm.sade.hakuperusteet.auth.TokenAuthStrategy
import fi.vm.sade.hakuperusteet.db.HakuperusteetDatabase
import fi.vm.sade.hakuperusteet.domain._
import fi.vm.sade.hakuperusteet.email.{EmailSender, EmailTemplate, WelcomeValues}
import fi.vm.sade.hakuperusteet.google.GoogleVerifier
import fi.vm.sade.hakuperusteet.henkilo.{HenkiloClient, IfGoogleAddEmailIDP}
import fi.vm.sade.hakuperusteet.oppijantunnistus.OppijanTunnistus
import fi.vm.sade.hakuperusteet.util.{AuditLog, ConflictException, Translate, ValidationUtil}
import fi.vm.sade.hakuperusteet.validation.{ApplicationObjectValidator, UserValidator}
import org.json4s.JsonDSL._
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization._
import fi.vm.sade.hakuperusteet.domain.AbstractUser._
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}
import scalaz._

class SessionServlet(config: Config, db: HakuperusteetDatabase, oppijanTunnistus: OppijanTunnistus, verifier: GoogleVerifier, userValidator: UserValidator, applicationObjectValidator: ApplicationObjectValidator, emailSender: EmailSender) extends HakuperusteetServlet(config, db, oppijanTunnistus, verifier) with ValidationUtil {
  case class UserDataResponse(field: String, value: SessionData)

  val henkiloClient = HenkiloClient.init(config)
  post("/authenticate") {
    if(!isAuthenticated || TokenAuthStrategy.hasTokenInRequest(request)) {
      authenticate()
    }
    failUnlessAuthenticated()
    returnUserData
  }

  get("/session") {
    failUnlessAuthenticated()
    returnUserData
  }

  post("/logout") {
    logOut()
    "{}"
  }

  post("/emailToken") {
    val params = parse(request.body).extract[Params]
    userValidator.parseEmailToken(params).bitraverse(
      errors => renderConflictWithErrors(errors),
      res => orderEmailToken(res._1, res._2, cookieToLang))
  }

  post("/userData") {
    failUnlessAuthenticated()
    val params = parse(request.body).extract[Params]
    userValidator.parseUserDataWithoutEmailAndIdpentityid(params).bitraverse(
      errors => renderConflictWithErrors(errors),
      partialUserData => createNewUser(user, partialUserData(user.email, user.idpentityid, cookieToLang)))
  }

  post("/educationData") {
    failUnlessAuthenticated()
    val params = parse(request.body).extract[Params]
    userDataFromSession match {
      case userData: User => applicationObjectValidator.parseApplicationObjectWithoutPersonOid(params).bitraverse(
          errors => renderConflictWithErrors(errors),
          partialEducation => addNewEducation(user, userData, partialEducation(userData.personOid.getOrElse(halt(500))))
        )
      case userData: PartialUser =>
        logger.error(s"session contained a partial user ${userData.email}")
        halt(500)
    }
  }

  private def returnUserData = {
    db.findUser(user.email) match {
      case Some(u : User) =>
        val educations = db.run(db.findApplicationObjects(u)).toList
        val payments = db.findPayments(u).toList
        write(SessionData(user, Some(u), educations, payments))
      case Some(u : PartialUser) =>
        val payments = db.findPayments(u).toList
        write(SessionData(user, Some(u), List(), payments))
      case _ => write(SessionData(user, None, List.empty, List.empty))
    }
  }

  def renderConflictWithErrors(errors: NonEmptyList[String]) = halt(status = 409, body = compact(render("errors" -> errors.list)))

  def orderEmailToken(email: String, hakukohdeOid: String, uiLang: String) =
    Try(oppijanTunnistus.createToken(email, hakukohdeOid, uiLang)) match {
      case Success(token) =>
        logger.info(s"Sending token to $email with value $token")
        halt(status = 200, body = compact(render(Map("status" -> "ok"))))
      case Failure(f) => logAndHalt("Oppijantunnistus.createToken error", f)
    }

  def createNewUser(session: Session, userData: User) = {
    logger.info(s"Updating userData: $userData")
    val newUser = upsertUserToHenkilo(userData)
    db.findUser(newUser.email) match {
      case Some(u : User) =>
        logger.error("Updating existing user data is not allowed!")
        halt(500)
      case Some(u: PartialUser) =>
        val userWithId = db.insertUserDetails(userData.copy(id = u.id))
        sendEmail(newUser)
        AuditLog.auditPostUserdata(userData)
        halt(status = 200, body = write(UserDataResponse("sessionData", SessionData(session, userWithId, List.empty, List.empty))))
      case _ =>
        val userWithId = db.upsertUser(newUser)
        sendEmail(newUser)
        AuditLog.auditPostUserdata(userData)
        halt(status = 200, body = write(UserDataResponse("sessionData", SessionData(session, userWithId, List.empty, List.empty))))
    }
  }

  def addNewEducation(session: Session, userData: User, education: ApplicationObject) = {
    logger.info(s"Updating education: $education")
    db.run(db.upsertApplicationObject(education))
    val educations = db.run(db.findApplicationObjects(userData)).toList
    val payments = db.findPayments(userData).toList
    AuditLog.auditPostEducation(userData, education)
    halt(status = 200, body = write(UserDataResponse("sessionData", SessionData(session, Some(userData), educations, payments))))
  }

  private def sendEmail(newUser: User) = {
    val p = WelcomeValues(newUser.fullName)
    emailSender.send(newUser.email, Translate("email.welcome",getUserLang(newUser),"title"), EmailTemplate.renderWelcome(p, getUserLang(newUser)))
  }

  def upsertUserToHenkilo(userData: User) = Try(henkiloClient.upsertHenkilo(IfGoogleAddEmailIDP(userData))) match {
      case Success(u) => userData.copy(personOid = Some(u.personOid))
      case Failure(t) if t.isInstanceOf[ConflictException] =>
        val msg = t.getMessage
        logger.error(s"Henkilopalvelu conflict (409) for email ${userData.email} with message $msg")
        renderConflictWithErrors(NonEmptyList[String](msg))
      case Failure(t) if t.isInstanceOf[IllegalArgumentException] =>
        logAndHalt("error parsing user", t)
      case Failure(t) => logAndHalt(s"Henkilopalvelu server error for email ${userData.email}", t)
    }

  private def logAndHalt(msg: String, t: Throwable) = {
    logger.error(msg, t)
    halt(status = 500, body = compact(render("errors" -> List(t.getMessage))))
  }
}
