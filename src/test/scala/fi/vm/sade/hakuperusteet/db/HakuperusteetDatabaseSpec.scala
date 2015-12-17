package fi.vm.sade.hakuperusteet.db

import java.util.Date

import com.typesafe.scalalogging.LazyLogging
import fi.vm.sade.hakuperusteet.domain.{OppijaToken, Payment, PaymentStatus, User}
import fi.vm.sade.hakuperusteet.{Configuration, DBSupport, HakuperusteetTestServer}
import org.scalatest.{BeforeAndAfterAll, FlatSpec, Matchers}

class HakuperusteetDatabaseSpec extends FlatSpec with LazyLogging with Matchers with BeforeAndAfterAll with DBSupport {
  behavior of "HakuperusteetDatabase"

  val config = Configuration.props
  val db = HakuperusteetDatabase(config)

  override def beforeAll() = {
    HakuperusteetTestServer.cleanDB()
  }

  it should "should create new session" in {
    val user = new User(None, Some("personOid.1.1.1"), "", Some(""), Some(""), Some(new Date()), None, OppijaToken, Some(""), Some(""), Some(""), "en")
    db.upsertUser(user)

    db.findPayments(user).length shouldEqual 0
    val p = Payment(None, "personOid.1.1.1", new Date(), "refNo", "orderNo", "paymCallId", PaymentStatus.ok, None)
    db.upsertPayment(p)
    db.findPayments(user).length shouldEqual 1
  }
}
