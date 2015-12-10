package fi.vm.sade.hakuperusteet.admin

import java.util.Date
import java.util.concurrent.{Executors, TimeUnit}

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import fi.vm.sade.hakuperusteet.db.HakuperusteetDatabase
import fi.vm.sade.hakuperusteet.domain.PaymentStatus.PaymentStatus
import fi.vm.sade.hakuperusteet.domain._
import fi.vm.sade.hakuperusteet.util.PaymentUtil
import fi.vm.sade.hakuperusteet.vetuma.{CheckResponse, VetumaCheck}
import slick.driver.PostgresDriver.api._

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


class PaymentSynchronization(config: Config, db: HakuperusteetDatabase) extends LazyLogging {
  val vetumaCheck = new VetumaCheck(config, "PAYMENT-APP2", "P")
  val vetumaQueryHost = s"${config.getString("vetuma.host")}Query"

  val scheduler = Executors.newScheduledThreadPool(1)
  def start = scheduler.scheduleWithFixedDelay(checkPaymentSynchronizations, 1, TimeUnit.MINUTES.toSeconds(1), SECONDS)

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
    val hadPaid = PaymentUtil.hasPaid(payments)
    val paymentAndCheckOption = payments.map(payment => (payment, vetumaCheck.doVetumaCheck(payment.paymCallId, new Date(), u.uiLang)))
    .filter(_._2.isDefined)

    val newPayments = updatePaymentsAndCreateEvents(paymentAndCheckOption.map(pAndC => (pAndC._1, pAndC._2.get)))
    val hasPaid = PaymentUtil.hasPaid(newPayments)
    if(hadPaid != hasPaid) {
      logger.info(s"$u payment status has changed from $hadPaid to $hasPaid starting synchronization.")
      newPayments.filter(p => p.hakemusOid.isDefined).foreach(p => db.insertPaymentSyncRequest(u, p))
      (u) match {
        case u: User =>
          db.run(db.findApplicationObjects(u) flatMap (aos => DBIO.sequence(aos map (a => db.insertSyncRequest(u, a)))), 5 seconds)
        case _ =>
          logger.debug(s"$u is partial user and hence doesnt have applications so skipping application sync!")
      }
    }
  }

  private def updatePaymentsAndCreateEvents(paymentAndChecks: Seq[(Payment, CheckResponse)]): Seq[Payment] = paymentAndChecks.map(pAndC => {
    val (payment, check) = pAndC
    if(isValidVetumaCheck(check)) {
      updatePaymentAndCreateEvent(payment, check)
    } else {
      logger.error(s"Vetuma check returned ERROR status to $payment")
      payment
    }
  })

  private def updateOriginalPaymentIfStatusIsOk(payment: Payment, newStatus:PaymentStatus): Payment = {
    if(PaymentStatus.ok == newStatus) {
      db.upsertPayment(payment.copy(status = newStatus)).get
    } else {
      payment
    }
  }

  private def updatePaymentAndCreateEvent(payment: Payment, check: CheckResponse): Payment = {
    val newStatus: PaymentStatus = vetumaPaymentStatusToPaymentStatus(check.paymentStatus)
    val oldStatus = payment.status
    // TODO: Update original payment, code below! This change is for production dry run.
    val p = updateOriginalPaymentIfStatusIsOk(payment,newStatus)
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
