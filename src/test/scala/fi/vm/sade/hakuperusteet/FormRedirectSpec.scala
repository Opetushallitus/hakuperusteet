package fi.vm.sade.hakuperusteet

import java.util.Date

import fi.vm.sade.hakuperusteet.domain._
import fi.vm.sade.hakuperusteet.tarjonta.{ApplicationSystem, Tarjonta}
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.scalatra.HaltException
import org.scalatra.test.scalatest.ScalatraSuite

@RunWith(classOf[JUnitRunner])
class FormRedirectSpec extends FunSuite with ScalatraSuite with ServletTestDependencies {

  val formRedirect = new FormRedirectServlet(config, database, oppijanTunnistus, verifier, signer, countries, Mockito.mock(classOf[Tarjonta]))
  val rnd_number = scala.util.Random.nextInt(10000000)
  val email = s"test_$rnd_number@test.com"
  val userOid: Some[Oid] = Some(s"person_$rnd_number")
  val testUser = User(None, userOid, email, Some("firstName"), Some("lastName"),
    Some(new Date(100)), None, Google, Some("1"), Some("1"), Some("fi"), "fi")
  database.upsertUser(testUser)

  val appObject = ApplicationObject(None, userOid.get, "hakukohdeOid", "hakuOid", "educationLevel", "fi")
  val appSystem = ApplicationSystem("hakuOid", Some("formUri"), true, List())

  test("it should halt on payment conflict") {
    val res = formRedirect.doRedirect(testUser, appObject, appSystem, "educationLevel")
    res should equal(Left(409))
  }

  test("it should redirect on valid payment") {
    database.upsertPayment(Payment(None, userOid.get, new Date(1000000), "", "", "", PaymentStatus.ok, None))
    val res = formRedirect.doRedirect(testUser, appObject, appSystem, "educationLevel")
    res.isRight shouldBe(true)
  }

}
