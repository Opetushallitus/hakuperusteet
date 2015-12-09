package fi.vm.sade.hakuperusteet.admin

import java.net.ConnectException
import java.util.Date

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import fi.vm.sade.hakuperusteet.admin.auth.{CasAuthenticationSupport, CasSessionDB}
import fi.vm.sade.hakuperusteet.db.HakuperusteetDatabase
import fi.vm.sade.hakuperusteet.domain._
import fi.vm.sade.hakuperusteet.email.EmailTemplate
import fi.vm.sade.hakuperusteet.henkilo.{HenkiloClient, IfGoogleAddEmailIDP}
import fi.vm.sade.hakuperusteet.koodisto.Countries
import fi.vm.sade.hakuperusteet.oppijantunnistus.OppijanTunnistus
import fi.vm.sade.hakuperusteet.util.{AuditLog, PaymentUtil, Translate, ValidationUtil}
import fi.vm.sade.hakuperusteet.validation.{ApplicationObjectValidator, PaymentValidator, UserValidator}
import fi.vm.sade.utils.cas.CasLogout
import org.json4s.JsonDSL._
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization._
import org.scalatra.ScalatraServlet
import org.scalatra.swagger.{AllowableValues, DataType, ModelProperty, Swagger, SwaggerSupport}
import slick.driver.PostgresDriver.api._

import scala.concurrent.duration._
import scala.io.Source
import scala.util.{Failure, Success, Try}
import scalaz.{NonEmptyList, _}

class AdminServlet(val resourcePath: String, protected val cfg: Config, oppijanTunnistus: OppijanTunnistus, userValidator: UserValidator, applicationObjectValidator: ApplicationObjectValidator, db: HakuperusteetDatabase, countries: Countries)(implicit val swagger: Swagger) extends ScalatraServlet with SwaggerRedirect with CasAuthenticationSupport with LazyLogging with ValidationUtil with SwaggerSupport {
  override protected def applicationDescription: String = "Admin API"
  val paymentValidator = PaymentValidator()
  val staticFileContent = Source.fromURL(getClass.getResource(resourcePath)).mkString
  override def realm: String = "hakuperusteet_admin"
  implicit val formats = fi.vm.sade.hakuperusteet.formatsUI
  val host = cfg.getString("hakuperusteet.cas.url")
  val henkiloClient = HenkiloClient.init(cfg)

  def checkOphUser() = {
    checkAuthentication()
    if(!user.roles.contains("APP_HAKUPERUSTEETADMIN_REKISTERINPITAJA")) {
      logger.error(s"User ${user.username} is unauthorized!")
      halt(401)
    }
  }
  def checkAuthentication() = {
    authenticate
    failUnlessAuthenticated

    if(!user.roles.contains("APP_HAKUPERUSTEETADMIN_CRUD")) {
      logger.error(s"User ${user.username} is unauthorized!")
      halt(401)
    }
  }

  get("/") {
    checkAuthentication()
    contentType = "text/html"
    staticFileContent
  }

  post("/") {
    val logoutRequest = params.getOrElse("logoutRequest",halt(500))
    CasLogout.parseTicketFromLogoutRequest(logoutRequest) match {
      case Some(ticket) => CasSessionDB.invalidate(ticket)
      case None => {
        logger.error(s"Invalid logout request: ${logoutRequest}")
        halt(500, "Invalid logout request!")
      }
    }
    halt(200)
  }

  get("/oppija/*") {
    checkAuthentication()
    contentType = "text/html"
    staticFileContent
  }

  registerModel(org.scalatra.swagger.Model(
    id = "User",
    name = "User",
    qualifiedName = None,
    description = None,
    properties = List(
      "id" -> ModelProperty(
        `type` = DataType.String,
        position = 0,
        required = false,
        description = None),
      "personOid" -> ModelProperty(
        `type` = DataType.String,
        position = 1,
        required = false,
        description = None),
      "email" -> ModelProperty(
        `type` = DataType.String,
        position = 2,
        required = true,
        description = None),
      "firstName" -> ModelProperty(
        `type` = DataType.String,
        position = 3,
        required = false,
        description = None),
      "lastName" -> ModelProperty(
        `type` = DataType.String,
        position = 4,
        required = false,
        description = None),
      "birthDate" -> ModelProperty(
        `type` = DataType.Date,
        position = 5,
        required = false,
        description = None),
      "personId" -> ModelProperty(
        `type` = DataType.String,
        position = 6,
        required = false,
        description = None),
      "idpentityid" -> ModelProperty(
        `type` = DataType.String,
        allowableValues = AllowableValues(List("google", "oppijaToken")),
        position = 7,
        required = true,
        description = None),
      "gender" -> ModelProperty(
        `type` = DataType.String,
        position = 8,
        required = false,
        description = None),
      "nativeLanguage" -> ModelProperty(
        `type` = DataType.String,
        position = 9,
        required = false,
        description = None),
      "nationality" -> ModelProperty(
        `type` = DataType.String,
        position = 10,
        required = false,
        description = None)
    )
  ))

  get("/api/v1/admin", operation(apiOperation[List[User]]("getUsers")
    summary "Search users"
    notes "Search users by name or email."
    parameter queryParam[Option[String]]("search").description("Search term"))) {
    checkAuthentication()
    contentType = "application/json"
    val search = params.getOrElse("search", halt(400)).toLowerCase
    // TODO What do we want to search here? Do optimized query when search terms are decided!
    write(db.allUsers.filter(u => search.isEmpty || u.email.toLowerCase.contains(search) || (u.fullName).toLowerCase.contains(search)))
  }

  get("/d27db1a1-eef3-48f6-84f7-007655c2413f") {
    checkAuthentication()
    contentType = "text/html"
    staticFileContent
  }

  get("/api/v1/admin/users_with_drastic_payment_changes/") {
    checkOphUser()
    contentType = "application/json"
    halt(status = 200, body = write(db.findAllUsersAndPaymentsWithDrasticallyChangedPaymentStates.map(entry => {
      val (user, payments) = entry

      val oldest = PaymentUtil.hasPaidWithTheseStatuses(payments.map(db.oldestPaymentStatuses))
      val newest = PaymentUtil.hasPaidWithTheseStatuses(payments.map(db.newestPaymentStatuses))
      Map("user" -> user, "payments" -> payments, "old_state" -> oldest, "new_state" -> newest)
    })))
  }

  post("/api/v1/admin/payment") {
    checkOphUser()
    contentType = "application/json"
    val params = parse(request.body).extract[Params]
    paymentValidator.parsePaymentWithoutTimestamp(params).bitraverse(
      errors => renderConflictWithErrors(errors),
      partialPayment => {
        val paymentWithoutTimestamp = partialPayment(new Date())

        val u = db.findUserByOid(paymentWithoutTimestamp.personOid).get
        val oldPayment = db.findPaymentByOrderNumber(u, paymentWithoutTimestamp.orderNumber).get
        val payment = partialPayment(oldPayment.timestamp)
        db.upsertPayment(payment)
        AuditLog.auditAdminPayment(user.oid, u, payment)
        db.insertPaymentSyncRequest(u, payment)
        (u) match {
          case u: User =>
            halt(status = 200, body = write(syncAndWriteResponse(u)))
          case u: PartialUser =>
            halt(status = 200, body = write(fetchPartialUserData(u)))
        }

      }
    )
  }

  get("/api/v1/admin/:personoid") {
    checkAuthentication()
    contentType = "application/json"
    val personOid = params("personoid")
    val user = db.findUserByOid(personOid)
    user match {
      case Some(u: User) => write(fetchUserData(u))
      case Some(u: PartialUser) => write(fetchPartialUserData(u))
      case _ => halt(status = 404, body = s"User ${personOid} not found")
    }
  }

  post("/api/v1/admin/user") {
    checkAuthentication()
    contentType = "application/json"
    val params = parse(request.body).extract[Params]
    userValidator.parseUserData(params).bitraverse(
      errors => renderConflictWithErrors(errors),
      newUserData => {
        halt(status = 200, body = write(saveUserData(newUserData)))
      }
    )
  }

  post("/api/v1/admin/applicationobject") {
    checkAuthentication()
    contentType = "application/json"
    val params = parse(request.body).extract[Params]
    applicationObjectValidator.parseApplicationObject(params).bitraverse(
      errors => renderConflictWithErrors(errors),
      education => {
        val u = db.findUserByOid(education.personOid) match {
          case Some(u: User) => u
          case Some(u: PartialUser) =>
            val msg = s"Tried to submit applications to partial user ${education.personOid}"
            logger.error(msg)
            halt(400, msg)
          case None =>
            val msg = s"No user ${education.personOid} found"
            logger.error(msg)
            halt(400, body = msg)
        }
        val oldAO = db.findApplicationObjectByHakukohdeOid(u, education.hakukohdeOid)
        db.run((db.upsertApplicationObject(education) >> (oldAO match {
          case Some(ao) if (paymentNowRequired(db.findPayments(u), ao, education)) =>
             sendPaymentInfoEmail(u, education.hakukohdeOid) match {
              case Success(_) => DBIO.successful(())
              case Failure(e) => DBIO.failed(e)
            }
          case _ => DBIO.successful(())
        })).transactionally.asTry, 5 seconds) match {
          case Success(()) =>
            AuditLog.auditAdminPostEducation(user.oid, u, education)
            halt(200, body = write(syncAndWriteResponse(u)))
          case Failure(e) =>
            logger.error("", e)
            halt(500, body = e.getMessage)
        }
      }
    )
  }

  error {
    case e: Throwable =>
      logger.error("uncaught exception", e)
      halt(500)
  }

  private def sendPaymentInfoEmail(user: User, hakukohdeOid: String): Try[Unit] = {
    logger.info(s"sending payment info email to ${user.email}")
    oppijanTunnistus.sendToken(hakukohdeOid, user.email,
      Translate("email", "paymentInfo", user.lang, "title"),
      EmailTemplate.renderPaymentInfo(user.fullName, user.lang),
      user.lang) match {
      case Success(_) => Success(())
      case Failure(e) => Failure(new RuntimeException(s"sending payment info email to ${user.email} failed", e))
    }
  }

  private def paymentNowRequired(payments: Seq[Payment], oldAO: ApplicationObject, newAO: ApplicationObject) =
    (payments.forall(p => p.status != PaymentStatus.ok && p.status != PaymentStatus.started)
      && !countries.shouldPay(oldAO.educationCountry, oldAO.educationLevel)
      && countries.shouldPay(newAO.educationCountry, newAO.educationLevel))

  private def upsertAndAudit(userData: User) = {
    db.insertUserDetails(userData)
    AuditLog.auditAdminPostUserdata(user.oid, userData)
    syncAndWriteResponse(userData)
  }

  private def saveUpdatedUserData(updatedUserData: User) = {
    Try(henkiloClient.upsertHenkilo(IfGoogleAddEmailIDP(updatedUserData))) match {
      case Success(_) => upsertAndAudit(updatedUserData)
      case Failure(t) if t.isInstanceOf[ConnectException] =>
        logger.error(s"admin-Henkilopalvelu connection error for email ${updatedUserData.email}", t)
        halt(500, body = "admin-Henkilopalvelu connection error")
      case Failure(t) if t.isInstanceOf[IllegalArgumentException] =>
        logger.error("error parsing user", t)
        halt(500, body = t.getMessage)
      case Failure(t) =>
        val error = s"admin-Henkilopalvelu upsert failed for email ${updatedUserData.email}"
        logger.error(error, t)
        renderConflictWithErrors(NonEmptyList[String](error))
    }
  }

  private def saveUserData(newUserData: User) = {
    db.findUserByOid(newUserData.personOid.getOrElse(halt(500, body = "PersonOid is mandatory"))) match {
      case Some(oldUserData: User) =>
        val updatedUserData = oldUserData.copy(firstName = newUserData.firstName, lastName = newUserData.lastName, birthDate = newUserData.birthDate, personId = newUserData.personId, gender = newUserData.gender, nativeLanguage = newUserData.nativeLanguage, nationality = newUserData.nationality)
        saveUpdatedUserData(updatedUserData)
      case _ => halt(404)
    }
  }

  private def fetchPartialUserData(u: PartialUser): PartialUserData = {
    val payments = db.findPayments(u)
    PartialUserData(user, u, PaymentUtil.sortPaymentsByStatus(payments).map(decorateWithPaymentHistory),PaymentUtil.hasPaid(payments))
  }

  private def decorateWithPaymentHistory(p: Payment) = p.copy(history = Some(db.findStateChangingEventsForPayment(p).sortBy(_.created)))

  private def fetchUserData(u: User): UserData = {
    val payments = db.findPayments(u)
    UserData(user, u, db.findApplicationObjects(u), PaymentUtil.sortPaymentsByStatus(payments).map(decorateWithPaymentHistory),PaymentUtil.hasPaid(payments))
  }

  private def syncAndWriteResponse(u: User) = {
    val data = fetchUserData(u)
    insertSyncRequests(data)
    data
  }

  private def insertSyncRequests(u: UserData) = u.applicationObject.foreach(db.insertSyncRequest(u.user, _))

  def renderConflictWithErrors(errors: NonEmptyList[String]) = halt(status = 409, body = compact(render("errors" -> errors.list)))
}
