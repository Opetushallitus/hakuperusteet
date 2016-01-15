package fi.vm.sade.hakuperusteet.db

import java.util.Date

import com.typesafe.scalalogging.LazyLogging
import fi.vm.sade.hakuperusteet.domain._
import fi.vm.sade.hakuperusteet.{DBSupport, _}
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

import scala.concurrent.duration._

class HakuperusteetDatabaseSpec extends FlatSpec with LazyLogging with Matchers with BeforeAndAfter with DBSupport {
  behavior of "HakuperusteetDatabase"

  val config = Configuration.props
  val db = HakuperusteetDatabase(config)

  before {
    HakuperusteetTestServer.cleanDB()
  }

  it should "create new session" in {
    val user = AbstractUser.user(None, Some("personOid.1.1.1"), "", Some(""), Some(""), Some(new Date()), None, OppijaToken, Some(""), Some(""), Some(""), "en")
    db.upsertUser(user)

    db.findPayments(user).length shouldEqual 0
    val p = Payment(None, "personOid.1.1.1", new Date(), "refNo", "orderNo", "paymCallId", PaymentStatus.ok, None)
    db.upsertPayment(p)
    db.findPayments(user).length shouldEqual 1
  }

  it should "save email in lowercase" in {
    val someUser = Users.generateRandomUser
    val newEmail = "Capital" + someUser.email
    val u = db.upsertPartialUser(someUser.copy(email = newEmail)).get
    u.email shouldEqual newEmail.toLowerCase()
  }

  it should "return users having application options matching admin's organization tree" in {
    val hakuOid = "hakuoid"
    val hakukohdeA: String = "oid.A"
    val hakukohdeB: String = "oid.B"
    val upsertAo = (user: AbstractUser, hakukohdeOid: String) => db.run(db.upsertApplicationObject(ApplicationObject(None, user.personOid.get, hakukohdeOid, hakuOid, "education-level", "fi")))
    val upsertUser = (henkiloOid: String) => db.upsertUser(AbstractUser.user(None, Some(henkiloOid), s"$henkiloOid@example.com", Some(""), Some(""), Some(new Date()), None, OppijaToken, Some(""), Some(""), Some(""), "en"))

    // "Ulkolomakkeella" täyttetyt hakemukset sisältävät hakukohteet hakuperusteissa
    upsertUser("1").foreach(user => {
      upsertAo(user, hakukohdeA)
      upsertAo(user, hakukohdeB)
    })
    upsertUser("2").foreach(upsertAo(_, hakukohdeB))
    upsertUser("3").foreach(upsertAo(_, hakukohdeA))

    // "Sisäisellä" hakulomakkeella täytetty, ei hakukohteita hakuperusteissa
    upsertUser("4")

    val allUsersForOph = db.allUsers(OphOrganizationalAccess())
    allUsersForOph.length shouldEqual 4
    allUsersForOph.map(_.personOid.get) should (contain("1") and contain("2") and contain("3") and contain("4"))

    val usersUsingSingleApplicationOption = db.allUsers(NonOphOrganizationalAccess(List(hakukohdeA)))
    usersUsingSingleApplicationOption.length shouldEqual 2
    usersUsingSingleApplicationOption.map(_.personOid.get) should (contain("1") and contain("3"))

    val usersFromMixedApplicationOptions = db.allUsers(NonOphOrganizationalAccess(List(hakukohdeA, hakukohdeB, "oid.X")))
    usersFromMixedApplicationOptions.length shouldEqual 3
    usersFromMixedApplicationOptions.map(_.personOid.get) should (contain("1") and contain("2") and contain("3"))

    db.allUsers(NonOphOrganizationalAccess(List("mismatching-oid"))).length shouldEqual 0

    db.allUsers(NonOphOrganizationalAccess(List())).length shouldEqual 0
  }
}
