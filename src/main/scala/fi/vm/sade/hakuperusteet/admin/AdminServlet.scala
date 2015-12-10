package fi.vm.sade.hakuperusteet.admin

import java.net.ConnectException

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import fi.vm.sade.hakuperusteet.admin.auth.{CasAuthenticationSupport, CasSessionDB}
import fi.vm.sade.hakuperusteet.db.HakuperusteetDatabase
import fi.vm.sade.hakuperusteet.db.HakuperusteetDatabase.toDBIO
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

import scala.concurrent.ExecutionContext.Implicits.global
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
      case Some(u: User) => write(db.run(fetchUserData(u), 5 seconds))
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
      education => db.run((for {
        u <- findFullUser(education.personOid)
        oldAO <- db.findApplicationObjectByHakukohdeOid(u, education.hakukohdeOid)
        _ <- db.upsertApplicationObject(education)
        _ <- toDBIO(sendPaymentInfoEmailIfPaymentNowRequired(u, oldAO, education))
        userData <- syncAndWriteResponse(u)
      } yield userData).transactionally.asTry, 5 seconds) match {
        case Success(userData: UserData) =>
          AuditLog.auditAdminPostEducation(user.oid, userData.user, education)
          halt(200, body = write(userData))
        case Failure(e: IllegalArgumentException) =>
          logger.error("bad application object update request", e)
          halt(400, body = e.getMessage)
        case Failure(e) =>
          logger.error("", e)
          halt(500, body = e.getMessage)
      }
    )
  }

  error {
    case e: Throwable =>
      logger.error("uncaught exception", e)
      halt(500)
  }

  private def findFullUser(personOid: String): DBIO[User] = db.findUserByOid(personOid) match {
    case Some(u: User) => DBIO.successful(u)
    case Some(u: PartialUser) => DBIO.failed(new IllegalArgumentException(s"${personOid} is a partial user"))
    case None => DBIO.failed(new IllegalArgumentException(s"no user ${personOid} found"))
  }

  private def sendPaymentInfoEmailIfPaymentNowRequired(u: User,
                                                       oldAO: Option[ApplicationObject],
                                                       newAO: ApplicationObject): Try[Unit] = oldAO match {
    case Some(ao) if (paymentNowRequired(db.findPayments(u), ao, newAO)) =>
      sendPaymentInfoEmail(u, newAO.hakukohdeOid)
    case _ => Success(())
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

  private def upsertAndAudit(userData: User): UserData = {
    db.insertUserDetails(userData)
    AuditLog.auditAdminPostUserdata(user.oid, userData)
    db.run(syncAndWriteResponse(userData), 5 seconds)
  }

  private def saveUpdatedUserData(updatedUserData: User): UserData = {
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

  private def saveUserData(newUserData: User): UserData = {
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

  private def fetchUserData(u: User): DBIO[UserData] =
    for {
      payments <- db.findPaymentsAction(u) map PaymentUtil.sortPaymentsByStatus
      applicationObjects <- db.findApplicationObjects(u)
    } yield UserData(user, u, applicationObjects, payments map decorateWithPaymentHistory, PaymentUtil.hasPaid(payments))

  private def syncAndWriteResponse(u: User): DBIO[UserData] =
    for {
      userData <- fetchUserData(u)
      _ <- DBIO.sequence(userData.applicationObject.map(db.insertSyncRequest(userData.user, _)))
    } yield userData

  def renderConflictWithErrors(errors: NonEmptyList[String]) = halt(status = 409, body = compact(render("errors" -> errors.list)))
}
