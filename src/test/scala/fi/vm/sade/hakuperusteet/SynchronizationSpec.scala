package fi.vm.sade.hakuperusteet

import java.util.Date
import java.util.concurrent.Executors

import fi.vm.sade.hakuperusteet.admin.Synchronization
import fi.vm.sade.hakuperusteet.domain._
import fi.vm.sade.hakuperusteet.domain.PaymentState.PaymentState
import fi.vm.sade.hakuperusteet.domain.PaymentStatus.PaymentStatus
import fi.vm.sade.hakuperusteet.hakuapp.HakuAppClient
import fi.vm.sade.hakuperusteet.tarjonta.Tarjonta
import org.http4s
import org.http4s.Status
import org.json4s.native.JsonMethods._
import org.junit.runner.RunWith
import org.mockito.{Mockito, Mock}
import org.mockito.Matchers._
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.scalatra.test.scalatest.ScalatraSuite
import fi.vm.sade.hakuperusteet.domain.User._

import scalaz.Failure

@RunWith(classOf[JUnitRunner])
class SynchronizationSpec extends FunSuite with ScalatraSuite with ServletTestDependencies {
  val hakuAppMock = Mockito.mock(classOf[HakuAppClient])
  val tarjonta = Mockito.mock(classOf[Tarjonta])
  val synchronization = new Synchronization(config, database, tarjonta, countries, null) {
    override val hakuAppClient = hakuAppMock
    def publicCheckForId(id: Int) = checkSynchronizationForId(id)
  }

  test("haku-app synchronization") {
    val personOid = "4.4.4.4"
    val hakemusOid = "1.1.1.1"
    val email = "e@mail.com"
    val user = database.findUser(email).orElse(database.upsertPartialUser(PartialUser(None, Some(personOid), email, IDPEntityId.oppijaToken, "en"))).get

    val payment1 = Payment(None, personOid, new Date(), "1234", "1234", "1234", PaymentStatus.error,Some(hakemusOid), None)
    database.upsertPayment(payment1)
    val sync = database.insertPaymentSyncRequest(user, payment1).get
    Mockito.when(hakuAppMock.updateHakemusWithPaymentState(anyString(), any[PaymentState])).thenReturn(http4s.Response(Status.Forbidden))
    synchronization.publicCheckForId(sync.id)
    Mockito.verify(hakuAppMock, Mockito.times(1)).updateHakemusWithPaymentState(anyString(), any[PaymentState])
  }
}
