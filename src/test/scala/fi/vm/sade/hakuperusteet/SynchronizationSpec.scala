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
import org.json4s.jackson.JsonMethods._
import org.junit.runner.RunWith
import org.mockito.{Mockito, Mock}
import org.mockito.ArgumentMatchers._
import org.scalatest.{BeforeAndAfterAll, FunSuite}
import org.scalatest.junit.JUnitRunner
import org.scalatra.test.scalatest.ScalatraSuite
import fi.vm.sade.hakuperusteet.domain.AbstractUser.PartialUser

import scalaz.Failure

@RunWith(classOf[JUnitRunner])
class SynchronizationSpec extends FunSuite with ScalatraSuite with ServletTestDependencies with BeforeAndAfterAll {
  val hakuAppMock = Mockito.mock(classOf[HakuAppClient])
  val tarjonta = Mockito.mock(classOf[Tarjonta])
  val hakumaksukausiServiceMock = Mockito.mock(classOf[HakumaksukausiService])
  val synchronization = new Synchronization(config, database, tarjonta, countries, null, hakumaksukausiServiceMock) {
    override val hakuAppClient = hakuAppMock
    def publicCheckForId(id: Int) = checkSynchronizationForId(id)
  }

  override def beforeAll() = {
    HakuperusteetTestServer.cleanDB()
  }

  test("haku-app synchronization") {
    val personOid = "4.4.4.4"
    val hakemusOid = "1.1.1.1"
    val email = "e@mail.com"
    val user = database.upsertPartialUser(AbstractUser.partialUser(None, Some(personOid), email, OppijaToken, "en")).get
    val payment1 = database.upsertPayment(Payment(None, personOid, new Date(), "1234", "1234", "1234", PaymentStatus.error, Hakumaksukausi.s2016, Some(hakemusOid))).get
    val sync = database.insertPaymentSyncRequest(user, payment1).get
    Mockito.when(hakuAppMock.updateHakemusWithPaymentState(anyString(), any[PaymentState])).thenReturn(http4s.Response(Status.Forbidden))
    Mockito.when(hakumaksukausiServiceMock.getHakumaksukausiForHakemus(hakemusOid)).thenReturn(Some(Hakumaksukausi.s2016))
    synchronization.publicCheckForId(sync.id)
    Mockito.verify(hakuAppMock, Mockito.times(1)).updateHakemusWithPaymentState(org.mockito.ArgumentMatchers.eq(hakemusOid), any[PaymentState])
  }

  test("haku-app synchronization - conflicting hakumaksukausi") {
    val personOid = "5.5.5.5"
    val hakemusOid = "2.2.2.2"
    val email = "f@mail.com"
    val user = database.upsertPartialUser(AbstractUser.partialUser(None, Some(personOid), email, OppijaToken, "en")).get
    val payment1 = database.upsertPayment(Payment(None, personOid, new Date(), "4567", "4567", "4567", PaymentStatus.error, Hakumaksukausi.k2017, Some(hakemusOid))).get
    val sync = database.insertPaymentSyncRequest(user, payment1).get
    Mockito.when(hakuAppMock.updateHakemusWithPaymentState(anyString(), any[PaymentState])).thenReturn(http4s.Response(Status.Forbidden))
    Mockito.when(hakumaksukausiServiceMock.getHakumaksukausiForHakemus(hakemusOid)).thenReturn(Some(Hakumaksukausi.s2016))
    synchronization.publicCheckForId(sync.id)
    Mockito.verify(hakuAppMock, Mockito.never()).updateHakemusWithPaymentState(org.mockito.ArgumentMatchers.eq(hakemusOid), any[PaymentState])
  }
}
