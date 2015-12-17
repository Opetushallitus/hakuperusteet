package fi.vm.sade.hakuperusteet

import java.util.Date
import fi.vm.sade.hakuperusteet.db.HakuperusteetDatabase
import fi.vm.sade.hakuperusteet.domain._
import fi.vm.sade.hakuperusteet.util.AuditLog

trait PaymentService {
  val db: HakuperusteetDatabase

  def findPayments(user: AbstractUser): Seq[Payment] =
    db.findPayments(user)

  def updatePayment(casSession: CasSession, user: AbstractUser, paymentWithoutTimestamp: ((Date) => Payment)): Unit = {
    val tmpPayment = paymentWithoutTimestamp(new Date())
    val oldPayment = db.findPaymentByOrderNumber(user.personOid.get, tmpPayment.orderNumber).get
    val payment = paymentWithoutTimestamp(oldPayment.timestamp)
    db.upsertPayment(payment)
    AuditLog.auditAdminPayment(casSession.oid, user, payment)
    db.insertPaymentSyncRequest(user, payment)
  }
}

object PaymentService {
  def apply(database: HakuperusteetDatabase) = new PaymentService {
    override val db: HakuperusteetDatabase = database
  }
}
