package fi.vm.sade.hakuperusteet.db

import java.util.Date

import com.typesafe.scalalogging.LazyLogging
import fi.vm.sade.hakuperusteet.domain._
import fi.vm.sade.hakuperusteet.{Configuration, DBSupport, HakuperusteetTestServer}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

class HakuperusteetDatabaseSpec extends FlatSpec with LazyLogging with Matchers with BeforeAndAfterAll with DBSupport {
  behavior of "HakuperusteetDatabase"

  val config = Configuration.props
  val db = HakuperusteetDatabase(config)

  override def beforeAll() = {
    HakuperusteetTestServer.cleanDB()
  }

  it should "create new session" in {
    val user = AbstractUser.user(None, Some("personOid.1.1.1"), "", Some(""), Some(""), Some(new Date()), None, OppijaToken, Some(""), Some(""), Some(""), "en")
    db.upsertUser(user)

    db.findPayments(user).length shouldEqual 0
    val p = Payment(None, "personOid.1.1.1", new Date(), "refNo", "orderNo", "paymCallId", PaymentStatus.ok, Hakumaksukausi.s2016, None)
    db.upsertPayment(p)
    db.findPayments(user).length shouldEqual 1
  }

  it should "save email in lowercase" in {
    val someUser = Users.generateRandomUser
    val newEmail = "Capital" + someUser.email
    val u = db.upsertPartialUser(someUser.copy(email = newEmail)).get
    u.email shouldEqual newEmail.toLowerCase()
  }
}
