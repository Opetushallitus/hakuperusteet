package fi.vm.sade.hakuperusteet

import java.util.Date
import javax.servlet.http.{HttpServletRequest, HttpServletResponse}

import fi.vm.sade.hakuperusteet.admin.AdminServlet
import fi.vm.sade.hakuperusteet.admin.auth.CasBasicAuthStrategy
import fi.vm.sade.hakuperusteet.domain._
import fi.vm.sade.hakuperusteet.oppijantunnistus.OppijanTunnistus
import fi.vm.sade.hakuperusteet.swagger.AdminSwagger
import fi.vm.sade.hakuperusteet.validation.{ApplicationObjectValidator, UserValidator}
import org.json4s.native.Serialization.{write, _}
import org.junit.runner.RunWith
import org.mockito.Matchers.any
import org.mockito.{Matchers, Mockito}
import org.scalatest.junit.JUnitRunner
import org.scalatest.{BeforeAndAfterEach, FunSuite}
import org.scalatra.test.scalatest.ScalatraSuite
import slick.dbio.DBIO

import scala.concurrent.duration._
import scala.util.{Failure, Success}

@RunWith(classOf[JUnitRunner])
class AdminServletSpec extends FunSuite with ScalatraSuite with ServletTestDependencies with BeforeAndAfterEach {
  implicit val swagger = new AdminSwagger
  implicit val formats = fi.vm.sade.hakuperusteet.formatsUI
  val stream = getClass.getResourceAsStream("/logoutRequest.xml")
  val logoutRequest = scala.io.Source.fromInputStream( stream ).mkString
  override val oppijanTunnistus = Mockito.mock(classOf[OppijanTunnistus])
  val db = Mockito.spy(database)

  val contentTypeJson: Map[String, String] = Map("Content-Type" -> "application/json")
  val user = User(None, Some("1.2.246.562.24.00000000000"), "test@example.com", Some("firstName"),
    Some("lastName"), Some(new Date()), None, OppijaToken, Some("1"), Some("102"), Some("102"), "fi")
  val bachelors = "102"
  val argentina = "032"
  val finland = "246"
  val aoCountryArgentina = ApplicationObject(None, user.personOid.get, "1.2.246.562.20.00000000000", "1.2.246.562.5.00000000000", bachelors, argentina)
  val aoCountryFinland = aoCountryArgentina.copy(educationCountry = finland)
  val okPayment = Payment(None, user.personOid.get, new Date(), "reference", "orderNumber", "paymentCallId", PaymentStatus.ok, None)
  val s = new AdminServlet("/webapp-admin/index.html",config, oppijanTunnistus, UserValidator(countries,languages), ApplicationObjectValidator(countries,educations), db, countries) {
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
    Mockito.reset(oppijanTunnistus)
    Mockito.reset(db)
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
      db.run(db.findApplicationObjectByHakukohdeOid(user, aoCountryArgentina.hakukohdeOid), 5 seconds).isDefined should be (false)
    }
  }

  test("insert new application object if no found") {
    db.upsertUser(user)
    post("/api/v1/admin/applicationobject", write(aoCountryArgentina), contentTypeJson) {
      status should equal(200)
      val ao = db.run(db.findApplicationObjectByHakukohdeOid(user, aoCountryArgentina.hakukohdeOid), 5 seconds)
      ao.isDefined should be (true)
      ao.get.educationCountry should equal(aoCountryArgentina.educationCountry)
    }
  }

  test("send email if payment is required after update") {
    db.upsertUser(user)
    val ao = db.run(db.upsertApplicationObject(aoCountryFinland), 10 seconds).get
    Mockito.when(oppijanTunnistus.sendToken(any[String], any[String], any[String], any[String], any[String]))
      .thenReturn(Success(()))
    post("/api/v1/admin/applicationobject", write(aoCountryArgentina.copy(id = ao.id)), contentTypeJson) {
      status should equal(200)
      read[UserData](body).applicationObject.head.educationCountry should equal(aoCountryArgentina.educationCountry)
      val ao = db.run(db.findApplicationObjectByHakukohdeOid(user, aoCountryArgentina.hakukohdeOid), 5 seconds)
      ao.isDefined should be (true)
      ao.get.educationCountry should equal(aoCountryArgentina.educationCountry)
      Mockito.verify(oppijanTunnistus).sendToken(
        Matchers.eq[String](aoCountryArgentina.hakukohdeOid),
        Matchers.eq[String](user.email),
        Matchers.contains("Opintopolku"),
        Matchers.contains("{{verification-link}}"),
        Matchers.eq[String](user.lang))
    }
  }

  test("do not send email if ok payment exists") {
    db.upsertUser(user)
    val ao = db.run(db.upsertApplicationObject(aoCountryFinland), 10 seconds).get
    db.upsertPayment(okPayment)
    post("/api/v1/admin/applicationobject", write(aoCountryArgentina.copy(id = ao.id)), contentTypeJson) {
      status should equal(200)
      read[UserData](body).applicationObject.head.educationCountry should equal(aoCountryArgentina.educationCountry)
      val ao = db.run(db.findApplicationObjectByHakukohdeOid(user, aoCountryArgentina.hakukohdeOid), 5 seconds)
      ao.isDefined should be (true)
      ao.get.educationCountry should equal(aoCountryArgentina.educationCountry)
      Mockito.verify(oppijanTunnistus, Mockito.never).sendToken(any[String], any[String], any[String], any[String], any[String])
    }
  }

  test("roll back application object update if sending email fails") {
    db.upsertUser(user)
    val ao = db.run(db.upsertApplicationObject(aoCountryFinland), 10 seconds).get
    Mockito.when(oppijanTunnistus.sendToken(any[String], any[String], any[String], any[String], any[String]))
      .thenReturn(Failure(new RuntimeException("fail")))
    post("/api/v1/admin/applicationobject", write(aoCountryArgentina.copy(id = ao.id)), contentTypeJson) {
      status should equal(500)
      val ao = db.run(db.findApplicationObjectByHakukohdeOid(user, aoCountryArgentina.hakukohdeOid), 5 seconds)
      ao.isDefined should be (true)
      ao.get.educationCountry should equal(aoCountryFinland.educationCountry)
    }
  }

  test("do not send email if application object update fails") {
    db.upsertUser(user)
    val ao = db.run(db.upsertApplicationObject(aoCountryFinland), 10 seconds).get
    Mockito.when(db.upsertApplicationObject(aoCountryArgentina.copy(id = ao.id)))
      .thenReturn(DBIO.failed(new RuntimeException("test fail")))
    post("/api/v1/admin/applicationobject", write(aoCountryArgentina.copy(id = ao.id)), contentTypeJson) {
      status should equal(500)
      Mockito.verify(oppijanTunnistus, Mockito.never)
        .sendToken(any[String], any[String], any[String], any[String], any[String])
    }
  }
}
