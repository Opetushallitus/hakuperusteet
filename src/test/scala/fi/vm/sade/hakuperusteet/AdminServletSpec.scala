package fi.vm.sade.hakuperusteet

import java.util.Date
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import fi.vm.sade.hakuperusteet
import fi.vm.sade.hakuperusteet.admin.AdminServlet
import fi.vm.sade.hakuperusteet.admin.auth.CasBasicAuthStrategy
import fi.vm.sade.hakuperusteet.domain._
import fi.vm.sade.hakuperusteet.henkilo.HenkiloClient
import fi.vm.sade.hakuperusteet.oppijantunnistus.OppijanTunnistus
import fi.vm.sade.hakuperusteet.swagger.AdminSwagger
import fi.vm.sade.hakuperusteet.tarjonta.Tarjonta
import fi.vm.sade.hakuperusteet.validation.{ApplicationObjectValidator, UserValidator}
import org.json4s.jackson.Serialization.{write, _}
import org.junit.runner.RunWith
import org.mockito.Matchers.any
import org.mockito.{AdditionalMatchers, Matchers, Mockito}
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterEach, FunSuite}
import org.scalatra.test.scalatest.ScalatraSuite
import slick.dbio.DBIO

import scala.concurrent.duration._
import scala.util.{Failure, Success}

@RunWith(classOf[JUnitRunner])
class AdminServletSpec extends FunSuite with ScalatraSuite with ServletTestDependencies with BeforeAndAfterEach {
  implicit val swagger = new AdminSwagger
  implicit val testExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val formats = fi.vm.sade.hakuperusteet.formatsUI
  val stream = getClass.getResourceAsStream("/logoutRequest.xml")
  val logoutRequest = scala.io.Source.fromInputStream( stream ).mkString
  val oppijanTunnistusMock = Mockito.mock(classOf[OppijanTunnistus])
  val dbSpy = Mockito.spy(database)
  val tarjontaMock = Mockito.mock(classOf[Tarjonta])
  val userServiceMock = UserService(testExecutionContext, HenkiloClient.init(config), dbSpy)
  val paymentServiceMock = PaymentService(dbSpy)
  val applicationObjectServiceMock = ApplicationObjectService(testExecutionContext, countries, dbSpy, oppijanTunnistusMock, paymentServiceMock, tarjontaMock)

  val contentTypeJson: Map[String, String] = Map("Content-Type" -> "application/json")
  val user = AbstractUser.user(None, Some("1.2.246.562.24.00000000000"), "test@example.com", Some("firstName"),
    Some("lastName"), Some(new Date()), None, OppijaToken, Some("1"), Some("102"), Some("102"), "fi")
  val bachelors = "102"
  val argentina = "032"
  val finland = "246"
  val aoInTarjonta = hakuperusteet.tarjonta.ApplicationObject("1.2.246.562.20.00000000000", "1.2.246.562.5.00000000000",
    new hakuperusteet.tarjonta.Nimi2(None, Some("hakukohteen nimi"), None),
    new hakuperusteet.tarjonta.Nimi2(Some("providerName"), Some("providerName"), Some("providerName")),
    List(),
    new hakuperusteet.tarjonta.Nimi2(Some("description"), Some("description"), Some("description")),
    "hakuaika",
    "status")
  val aoCountryArgentina = ApplicationObject(None, user.personOid.get, "1.2.246.562.20.00000000000", "1.2.246.562.5.00000000000", bachelors, argentina)
  val aoCountryFinland = aoCountryArgentina.copy(educationCountry = finland)
  val okPayment = Payment(None, user.personOid.get, new Date(), "reference", "orderNumber", "paymentCallId", PaymentStatus.ok, Hakumaksukausi.s2016, None)
  val s = new AdminServlet("/webapp-admin/index.html",config, UserValidator(countries,languages), ApplicationObjectValidator(countries,educations), userServiceMock, paymentServiceMock, applicationObjectServiceMock) {
    override protected def registerAuthStrategies = {
      scentry.register("CAS", app => new CasBasicAuthStrategy(app, cfg) {
        override def authenticate()(implicit request: HttpServletRequest, response: HttpServletResponse): Option[CasSession] = {
          Some(CasSession(None, "oid", "username", List("APP_HAKUPERUSTEETADMIN_CRUD"), "ticket"))
        }
      })
    }
  }

  override def beforeAll = {
    super.beforeAll()
    HakuperusteetTestServer.cleanDB()
  }

  override def afterEach = {
    Mockito.reset(oppijanTunnistusMock)
    Mockito.reset(dbSpy)
    HakuperusteetTestServer.cleanDB()
    super.afterEach()
  }

  addServlet(s, "/*")

  test("CAS logout") {
    post("/", Map("logoutRequest"->logoutRequest)) {
      status should equal (200)
    }
  }

  test("400 if user does not exist") {
    post("/api/v1/admin/applicationobject", write(aoCountryArgentina), contentTypeJson) {
      status should equal(400)
      dbSpy.run(dbSpy.findApplicationObjectByHakukohdeOid(user, aoCountryArgentina.hakukohdeOid)).isDefined should be (false)
    }
  }

  test("insert new application object if no found") {
    dbSpy.upsertUser(user)
    post("/api/v1/admin/applicationobject", write(aoCountryArgentina), contentTypeJson) {
      status should equal(200)
      val ao = dbSpy.run(dbSpy.findApplicationObjectByHakukohdeOid(user, aoCountryArgentina.hakukohdeOid))
      ao.get.educationCountry should equal(aoCountryArgentina.educationCountry)
    }
  }

  test("send email if payment is required after update") {
    dbSpy.upsertUser(user)
    val ao = dbSpy.run(dbSpy.upsertApplicationObject(aoCountryFinland)).get
    Mockito.when(tarjontaMock.getApplicationObject(aoCountryArgentina.hakukohdeOid))
      .thenReturn(aoInTarjonta)
    Mockito.when(oppijanTunnistusMock.sendToken(any[String], any[String], any[String], any[String], any[String], any[Long]))
      .thenReturn(Success(()))
    post("/api/v1/admin/applicationobject", write(aoCountryArgentina.copy(id = ao.id)), contentTypeJson) {
      status should equal(200)
      read[UserData](body).applicationObject.head.educationCountry should equal(aoCountryArgentina.educationCountry)
      val ao = dbSpy.run(dbSpy.findApplicationObjectByHakukohdeOid(user, aoCountryArgentina.hakukohdeOid))
      ao.get.educationCountry should equal(aoCountryArgentina.educationCountry)
      Mockito.verify(oppijanTunnistusMock).sendToken(
        Matchers.eq[String](aoCountryArgentina.hakukohdeOid),
        Matchers.eq[String](user.email),
        Matchers.contains("Opintopolku - täydennyspyyntö"),
        AdditionalMatchers.and(
          Matchers.contains("{{verification-link}}"),
          Matchers.contains("hakukohteen nimi")),
        Matchers.eq[String](user.lang),
        any[Long])
    }
  }

  test("do not send email if ok payment exists") {
    dbSpy.upsertUser(user)
    val ao = dbSpy.run(dbSpy.upsertApplicationObject(aoCountryFinland)).get
    dbSpy.upsertPayment(okPayment)
    post("/api/v1/admin/applicationobject", write(aoCountryArgentina.copy(id = ao.id)), contentTypeJson) {
      status should equal(200)
      println(body)
      read[UserData](body).applicationObject.head.educationCountry should equal(aoCountryArgentina.educationCountry)
      val ao = dbSpy.run(dbSpy.findApplicationObjectByHakukohdeOid(user, aoCountryArgentina.hakukohdeOid))
      ao.get.educationCountry should equal(aoCountryArgentina.educationCountry)
      Mockito.verify(oppijanTunnistusMock, Mockito.never).sendToken(any[String], any[String], any[String], any[String], any[String], any[Long])
    }
  }

  test("roll back application object update if sending email fails") {
    dbSpy.upsertUser(user)
    val ao = dbSpy.run(dbSpy.upsertApplicationObject(aoCountryFinland)).get
    Mockito.when(oppijanTunnistusMock.sendToken(any[String], any[String], any[String], any[String], any[String], any[Long]))
      .thenReturn(Failure(new RuntimeException("test fail")))
    post("/api/v1/admin/applicationobject", write(aoCountryArgentina.copy(id = ao.id)), contentTypeJson) {
      status should equal(500)
      val ao = dbSpy.run(dbSpy.findApplicationObjectByHakukohdeOid(user, aoCountryArgentina.hakukohdeOid))
      ao.get.educationCountry should equal(aoCountryFinland.educationCountry)
    }
  }

  test("do not send email if application object update fails") {
    dbSpy.upsertUser(user)
    val ao = dbSpy.run(dbSpy.upsertApplicationObject(aoCountryFinland)).get
    Mockito.when(dbSpy.upsertApplicationObject(aoCountryArgentina.copy(id = ao.id)))
      .thenReturn(DBIO.failed(new RuntimeException("test fail")))
    post("/api/v1/admin/applicationobject", write(aoCountryArgentina.copy(id = ao.id)), contentTypeJson) {
      status should equal(500)
      Mockito.verify(oppijanTunnistusMock, Mockito.never)
        .sendToken(any[String], any[String], any[String], any[String], any[String], any[Long])
    }
  }
}
