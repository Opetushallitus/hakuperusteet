package fi.vm.sade.hakuperusteet.admin

import java.util.Date
import java.util.concurrent.{TimeUnit, Executors}
import java.util.concurrent.TimeUnit._

import com.typesafe.config.{ConfigFactory, Config}
import com.typesafe.scalalogging.LazyLogging
import fi.vm.sade.hakuperusteet.db.HakuperusteetDatabase
import fi.vm.sade.hakuperusteet.domain.PaymentStatus.PaymentStatus
import fi.vm.sade.hakuperusteet.domain._
import fi.vm.sade.hakuperusteet.vetuma.{VetumaCheck, CheckResponse}


class PaymentSynchronization(config: Config, db: HakuperusteetDatabase) extends LazyLogging {
  val vetumaCheck = new VetumaCheck(config, "PAYMENT-APP2", "P")
  val vetumaQueryHost = s"${config.getString("vetuma.host")}Query"

  val scheduler = Executors.newScheduledThreadPool(1)
  def start = scheduler.scheduleWithFixedDelay(checkPaymentSynchronizations, 1, TimeUnit.HOURS.toSeconds(1), SECONDS)

  def checkPaymentSynchronizations = asSimpleRunnable { () =>
    db.findUnchekedPaymentsGroupedByPersonOid.foreach(r => {
      val (personOid, paymentIds) = r
      val u: AbstractUser = db.findUserByOid(personOid).get
      try {
        handleUserPayments(u, paymentIds.flatMap(p => db.findPayment(p._1)))
      } catch {
        case e:Throwable => logger.error(s"Vetuma check failed to $u!", e)
      }
    })
  }
  private def handleUserPayments(u: AbstractUser, payments: Seq[Payment]) = {
    val hadPaid = payments.exists(_.status.equals(PaymentStatus.ok))
    val paymentAndCheckOption = payments.map(payment => (payment, vetumaCheck.doVetumaCheck(payment.paymCallId, new Date(), u.uiLang).filter(isValidVetumaCheck)))

    val anyErrors = paymentAndCheckOption.find(p => p._2.isEmpty)
    anyErrors match {
      case Some(pAndC) =>
        logger.error(s"Checking payments for $u failed with ${pAndC._1}")
      case None =>
        val newPayments = updatePaymentsAndCreateEvents(paymentAndCheckOption.map(pAndC => (pAndC._1, pAndC._2.get)))
        val hasPaid = newPayments.exists(_.status.equals(PaymentStatus.ok))
        if(hadPaid != hasPaid) {
          logger.info(s"$u payment status has changed from $hadPaid to $hasPaid. Updating Haku-App.")
          newPayments.filter(p => p.hakemusOid.isDefined).foreach(p => db.insertPaymentSyncRequest(u, p))

        }
    }
  }

  private def updatePaymentsAndCreateEvents(paymentAndChecks: Seq[(Payment, CheckResponse)]): Seq[Payment] = paymentAndChecks.map(pAndC => updatePaymentAndCreateEvent(pAndC._1, pAndC._2))

  private def updatePaymentAndCreateEvent(payment: Payment, check: CheckResponse): Payment = {
    val newStatus = vetumaPaymentStatusToPaymentStatus(check.paymentStatus)
    val oldStatus = payment.status
    val p = db.upsertPayment(payment.copy(status = newStatus)).get
    db.insertEvent(PaymentEvent(None, payment.id.get, new Date(), check.timestmp, true, check.paymentStatus,
      Some(newStatus), Some(oldStatus)))
    p
  }


  private def vetumaPaymentStatusToPaymentStatus(paymentStatus: String): PaymentStatus = {
    paymentStatus match {
      case "OK_VERIFIED" => PaymentStatus.ok
      case "CANCELLED_OR_REJECTED" => PaymentStatus.cancel
      case "PROBLEM" => PaymentStatus.error
      case "UNKNOWN_PAYMENT" => PaymentStatus.unknown
      case _ => PaymentStatus.unknown
    }
  }

  private def isValidVetumaCheck(vetumaCheck: CheckResponse) = !"ERROR".equals(vetumaCheck.status)

  private def asSimpleRunnable(f: () => Unit) = new Runnable() {
    override def run() {
      f()
    }
  }
}
