package fi.vm.sade.hakuperusteet.admin

import java.util.Date

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import fi.vm.sade.hakuperusteet._
import fi.vm.sade.hakuperusteet.admin.auth.{CasAuthenticationSupport, CasSessionDB}
import fi.vm.sade.hakuperusteet.domain._
import fi.vm.sade.hakuperusteet.util.{AuditLog, ValidationUtil}
import fi.vm.sade.hakuperusteet.validation.{ApplicationObjectValidator, PaymentValidator, UserValidator}
import fi.vm.sade.utils.cas.CasLogout
import org.json4s.JsonDSL._
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization._
import org.scalatra.ScalatraServlet
import org.scalatra.swagger.{AllowableValues, DataType, ModelProperty, Swagger, SwaggerSupport}
import fi.vm.sade.hakuperusteet.domain.AbstractUser._
import scala.concurrent.ExecutionContext
import scala.io.Source
import scala.util.{Failure, Success}
import scalaz.{NonEmptyList, _}

class AdminServlet(val resourcePath: String,
                   protected val cfg: Config,
                   userValidator: UserValidator,
                   applicationObjectValidator: ApplicationObjectValidator,
                   userService: UserService,
                   paymentService: PaymentService,
                   applicationObjectService: ApplicationObjectService)
                  (implicit val swagger: Swagger,
                   implicit val executionContext: ExecutionContext) extends ScalatraServlet with SwaggerRedirect with CasAuthenticationSupport with LazyLogging with ValidationUtil with SwaggerSupport {
  override protected def applicationDescription: String = "Admin API"
  val paymentValidator = PaymentValidator()
  val staticFileContent = Source.fromURL(getClass.getResource(resourcePath)).mkString
  override def realm: String = "hakuperusteet_admin"
  implicit val formats = fi.vm.sade.hakuperusteet.formatsUI
  val host = cfg.getString("hakuperusteet.cas.url")

  def checkOphUser(): CasSession = {
    val casSession = checkAuthentication()
    if(!user.roles.contains("APP_HAKUPERUSTEETADMIN_REKISTERINPITAJA")) {
      logger.error(s"User ${user.username} is unauthorized!")
      halt(401)
    }
    casSession
  }

  def checkAuthentication(): CasSession = {
    authenticate
    failUnlessAuthenticated

    if(!user.roles.contains("APP_HAKUPERUSTEETADMIN_CRUD")) {
      logger.error(s"User ${user.username} is unauthorized!")
      halt(401)
    }
    user // return the implicit session for use in ExecutionContexts
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
        logger.error(s"Invalid logout request: $logoutRequest")
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
    val search = params.getOrElse("search", halt(400))
    write(userService.searchUsers(search))
  }

  get("/d27db1a1-eef3-48f6-84f7-007655c2413f") {
    checkAuthentication()
    contentType = "text/html"
    staticFileContent
  }

  get("/api/v1/admin/users_with_drastic_payment_changes/") {
    checkOphUser()
    contentType = "application/json"
    write(userService.findUsersWithDrasticallyChangedPaymentState())
  }

  post("/api/v1/admin/payment") {
    val casSession = checkOphUser()
    contentType = "application/json"
    val params = parse(request.body).extract[Params]
    paymentValidator.parsePaymentWithoutTimestamp(params).bitraverse(
      errors => renderConflictWithErrors(errors),
      partialPayment => {
        val tmpPayment = partialPayment(new Date())
        val user = userService.findByPersonOid(tmpPayment.personOid).get
        paymentService.updatePayment(casSession, user, partialPayment)
        user match {
          case u: User =>
            halt(status = 200, body = userService.syncAndWriteResponse(casSession, u) match {
              case userData: UserData => write(userData)
              case userData: PartialUserData => halt(500)
            })
          case u: PartialUser =>
            halt(status = 200, body = userService.fetchPartialUserData(casSession, u) match {
              case userData: PartialUserData => write(userData)
              case userData: UserData => halt(500)
            })
        }
      }
    )
  }

  get("/api/v1/admin/:personoid") {
    val casSession = checkAuthentication()
    contentType = "application/json"
    val personOid = params("personoid")
    userService.findUserData(casSession, personOid).map {
      case userData: UserData => write(userData)
      case partialUserData: PartialUserData => write(partialUserData)
    }.getOrElse(halt(404, body = s"user $personOid not found"))
  }

  post("/api/v1/admin/user") {
    val casSession = checkAuthentication()
    contentType = "application/json"
    val params = parse(request.body).extract[Params]
    userValidator.parseUserData(params).bitraverse(
      errors => renderConflictWithErrors(errors),
      newUserData => {
        halt(status = 200, body = userService.updateUser(casSession, newUserData) match {
          case userData: UserData => write(userData)
          case userData: PartialUserData => write(userData)
        })
      }
    )
  }

  post("/api/v1/admin/applicationobject") {
    val casSession = checkAuthentication()
    contentType = "application/json"
    val params = parse(request.body).extract[Params]
    applicationObjectValidator.parseApplicationObject(params).bitraverse(
      errors => renderConflictWithErrors(errors),
      education => {
        val user = userService.findByPersonOid(education.personOid) match {
          case Some(user) => user
          case None => halt(400, "bad application object request")
        }
        applicationObjectService.upsertApplicationObject(casSession, user, education) match {
          case Success(()) => {
            val userData = userService.fetchUserData(casSession, user) match {
              case userData: UserData => userData
              case userData: PartialUserData => halt(500)
            }
            AuditLog.auditAdminPostEducation(casSession.oid, userData.user, education)
            halt (200, body = write(userData))
          }
          case Failure(e: IllegalArgumentException) =>
            logger.error("bad application object update request", e)
            halt(400, body = e.getMessage)
          case Failure(e) =>
            logger.error("application object update transaction failed", e)
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

  def renderConflictWithErrors(errors: NonEmptyList[String]) = halt(status = 409, body = compact(render("errors" -> errors.list)))
}
