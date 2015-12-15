package fi.vm.sade.hakuperusteet.vetuma
import java.io.Serializable
import java.net.InetSocketAddress
import java.text.SimpleDateFormat
import java.util.{UUID, Date}
import java.util.concurrent.CyclicBarrier

import com.netaporter.uri.Uri._
import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import com.typesafe.config.{ConfigFactory, Config}
import fi.vm.sade.hakuperusteet.domain.PaymentStatus
import fi.vm.sade.hakuperusteet.domain.PaymentStatus._
import fi.vm.sade.hakuperusteet.{ServletTestDependencies, Configuration}
import fi.vm.sade.hakuperusteet.domain._
import org.joda.time.DateTime
import org.scalatest.{FlatSpec, Matchers}
import scala.collection.JavaConversions._

class PaymentSynchronizationSpec extends FlatSpec with Matchers with ServletTestDependencies {

  behavior of "PaymentSynchronization"


  it should "read payment history in right order" in {
    {
      val p = generatePaymentToUser(generateRandomUser).copy(status = PaymentStatus.ok, history = None)
      database.newestPaymentStatuses(p) shouldEqual PaymentStatus.ok
      database.oldestPaymentStatuses(p) shouldEqual PaymentStatus.ok
    }
    {
      val p = generatePaymentToUser(generateRandomUser).copy(status = PaymentStatus.ok, history = Some(Seq()))
      database.newestPaymentStatuses(p) shouldEqual PaymentStatus.ok
      database.oldestPaymentStatuses(p) shouldEqual PaymentStatus.ok
    }
    {
      val h = PaymentEvent(None, 0, now.toDate, None, true, "", old_status = Some(PaymentStatus.cancel), new_status = Some(PaymentStatus.error))
      val p = generatePaymentToUser(generateRandomUser).copy(status = PaymentStatus.ok, history = Some(Seq(h)))
      database.newestPaymentStatuses(p) shouldEqual PaymentStatus.error
      database.oldestPaymentStatuses(p) shouldEqual PaymentStatus.cancel
    }
    {
      val h_old = PaymentEvent(None, 0, now.minusHours(5).toDate, None, true, "", old_status = Some(PaymentStatus.cancel), new_status = Some(PaymentStatus.error))
      val h_obso = PaymentEvent(None, 0, now.minusHours(1).toDate, None, true, "", old_status = Some(PaymentStatus.error), new_status = Some(PaymentStatus.started))
      val h_new = PaymentEvent(None, 0, now.toDate, None, true, "", old_status = Some(PaymentStatus.started), new_status = Some(PaymentStatus.ok))
      val p = generatePaymentToUser(generateRandomUser).copy(status = PaymentStatus.ok, history = Some(Seq(h_old, h_obso,h_new)))
      database.newestPaymentStatuses(p) shouldEqual PaymentStatus.ok
      database.oldestPaymentStatuses(p) shouldEqual PaymentStatus.cancel
    }
  }


  it should "skip checking too recent payments (less than hour)" in {
    val someUser = database.upsertPartialUser(generateRandomUser).get
    val someNotTooRecentPayment = generatePaymentToUser(someUser).copy(timestamp = now.minusHours(4).toDate)
    database.upsertPayment(someNotTooRecentPayment)
    val someUnchekedPayment = database.findUnchekedPaymentsGroupedByPersonOid.get(someUser.personOid.get)

    someUnchekedPayment should not equal None
    someUnchekedPayment.get should not be empty

    // User has 2 payments but one is too recent, shouldnt check any of them
    val someTooRecentPayment = generatePaymentToUser(someUser).copy(timestamp = now.toDate)
    database.upsertPayment(someTooRecentPayment)
    val withTooRecentPayments = database.findUnchekedPaymentsGroupedByPersonOid.get(someUser.personOid.get)

    withTooRecentPayments shouldEqual None
  }

  it should "skip checking too old payments (more than 14 days)" in {
    val someUser = database.upsertPartialUser(generateRandomUser).get
    val someTooOldPayment = generatePaymentToUser(someUser).copy(timestamp = now.minusDays(15).toDate)
    val someRecentEnoughPayment = generatePaymentToUser(someUser).copy(timestamp = now.minusDays(13).toDate)
    database.upsertPayment(someTooOldPayment)
    database.upsertPayment(someRecentEnoughPayment)

    // User has 2 payments one is too old, should check only the newest
    val someUnchekedPayments = database.findUnchekedPaymentsGroupedByPersonOid.get(someUser.personOid.get)

    someUnchekedPayments should not equal None
    someUnchekedPayments.get should not be empty
    someUnchekedPayments.get.length shouldEqual 1

    val (_, _, timestamp) = someUnchekedPayments.get.head

    timestamp.getTime shouldEqual someRecentEnoughPayment.timestamp.getTime
  }

  // TODO: Add README about correctly setting timezone to docker
  val BECAUSE_EVERYBODYS_DOCKER_IS_IN_WRONG_TIMEZONE = 3

  it should "skip checking payment that is already checked today" in {
    val someUser = database.upsertPartialUser(generateRandomUser).get
    val personOid = someUser.personOid.get
    val someRecentEnoughPayment1 = database.upsertPayment(generatePaymentToUser(someUser).copy(status = PaymentStatus.error, timestamp = now.minusDays(6).toDate)).get
    val someRecentEnoughPayment2 = database.upsertPayment(generatePaymentToUser(someUser).copy(status = PaymentStatus.cancel, timestamp = now.minusDays(6).toDate)).get


    val event1 = PaymentEvent(None, someRecentEnoughPayment1.id.get, new Date, None, false, "PROBLEM", Some(PaymentStatus.error), Some(PaymentStatus.ok))
    val event3 = PaymentEvent(None, someRecentEnoughPayment1.id.get, now.minusHours(15).toDate, None, false, "CANCELLED_OR_REJECTED", Some(PaymentStatus.ok), Some(PaymentStatus.cancel))

    val event2 = PaymentEvent(None, someRecentEnoughPayment2.id.get, now.minusHours(25 + BECAUSE_EVERYBODYS_DOCKER_IS_IN_WRONG_TIMEZONE).toDate, None, false, "OK_VERIFIED", None, Some(PaymentStatus.ok))
    database.insertEvent(event1)
    database.insertEvent(event2)
    database.insertEvent(event3)

    val payments = database.findPayments(someUser)
    (payments.size) shouldEqual 2

    val uncheckedPayments = database.findUnchekedRecentEnoughPayments
    val withThisUser = uncheckedPayments.filter(a => a._2 == someUser.personOid.get)
    withThisUser.size shouldEqual 1

    val someUnchekedPayments = database.findUnchekedPaymentsGroupedByPersonOid.get(someUser.personOid.get)

    someUnchekedPayments should not equal None
    someUnchekedPayments.get.length shouldEqual 1
    val (id, _, _) = someUnchekedPayments.get.head
    id shouldEqual someRecentEnoughPayment2.id.get
  }

  it should "not include users without payments" in {
    val someUser = database.upsertPartialUser(generateRandomUser).get
    val someUnchekedPayments = database.findUnchekedPaymentsGroupedByPersonOid.get(someUser.personOid.get)
    someUnchekedPayments shouldEqual None
  }

}
