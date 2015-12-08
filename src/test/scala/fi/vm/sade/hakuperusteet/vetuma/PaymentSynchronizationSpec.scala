package fi.vm.sade.hakuperusteet.vetuma
import java.io.Serializable
import java.net.InetSocketAddress
import java.text.SimpleDateFormat
import java.util.Date
import java.util.concurrent.CyclicBarrier

import com.netaporter.uri.Uri._
import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import com.typesafe.config.{ConfigFactory, Config}
import fi.vm.sade.hakuperusteet.domain.IDPEntityId.IDPEntityId
import fi.vm.sade.hakuperusteet.domain.PaymentStatus
import fi.vm.sade.hakuperusteet.domain.PaymentStatus._
import fi.vm.sade.hakuperusteet.{ServletTestDependencies, Configuration}
import fi.vm.sade.hakuperusteet.domain._
import org.joda.time.DateTime
import org.scalatest.{FlatSpec, Matchers}
import scala.collection.JavaConversions._

class PaymentSynchronizationSpec extends FlatSpec with Matchers with ServletTestDependencies {

  behavior of "PaymentSynchronization"



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

  it should "skip checking too old payments (more than 40 days)" in {
    val someUser = database.upsertPartialUser(generateRandomUser).get
    val someTooOldPayment = generatePaymentToUser(someUser).copy(timestamp = now.minusDays(41).toDate)
    val someRecentEnoughPayment = generatePaymentToUser(someUser).copy(timestamp = now.minusDays(39).toDate)
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

  it should "skip checking payment that is already checked today" in {
    val someUser = database.upsertPartialUser(generateRandomUser).get
    val someRecentEnoughPayment1 = database.upsertPayment(generatePaymentToUser(someUser).copy(timestamp = now.minusDays(15).toDate)).get
    val someRecentEnoughPayment2 = database.upsertPayment(generatePaymentToUser(someUser).copy(timestamp = now.minusDays(15).toDate)).get
    val event1 = PaymentEvent(None, someRecentEnoughPayment1.id.get, new Date, None, false, "", None, None)
    val event2 = PaymentEvent(None, someRecentEnoughPayment2.id.get, now.minusHours(25).toDate, None, false, "", None, None)
    database.insertEvent(event1)
    database.insertEvent(event2)

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
