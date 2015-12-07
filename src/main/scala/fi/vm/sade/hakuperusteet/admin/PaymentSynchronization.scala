package fi.vm.sade.hakuperusteet.admin

import java.util.Date
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit._

import com.typesafe.config.{ConfigFactory, Config}
import com.typesafe.scalalogging.LazyLogging
import fi.vm.sade.hakuperusteet.db.HakuperusteetDatabase
import fi.vm.sade.hakuperusteet.domain.{PaymentStatus, PaymentEvent, Payment}
import fi.vm.sade.hakuperusteet.vetuma.{VetumaCheck, CheckResponse}


class PaymentSynchronization(config: Config, db: HakuperusteetDatabase) extends LazyLogging {
  val vetumaCheck = new VetumaCheck(config, "PAYMENT-APP2", "P")
  val vetumaQueryHost = s"${config.getString("vetuma.host")}Query"

  val scheduler = Executors.newScheduledThreadPool(1)

  def start = scheduler.scheduleWithFixedDelay(checkPaymentSynchronizations, 1, config.getDuration("admin.synchronization.interval", SECONDS), SECONDS)

  def checkPaymentSynchronizations = asSimpleRunnable { () =>
    db.findUnchekedPayments.foreach(id => db.findPayment(id) match {
      case Some(payment: Payment) =>
        try {
        val u = db.findUserByOid(payment.personOid) match {
          case Some(u) => handleVetumaCheckForPayment(payment,
            vetumaCheck.doVetumaCheck(payment.paymCallId, new Date(), u.uiLang, None).filter(isValidVetumaCheck))
          case None =>
            logger.error(s"No user found for $payment")
        }

        } catch {
          case e: Throwable => logger.error(s"$e")
        }
      case _ =>
        logger.debug("All payments up to date with Vetuma.")
    })
  }

  private def vetumaPaymentStatusToPaymentStatus(paymentStatus: String) = {
    paymentStatus match {
      case "OK_VERIFIED" => Some(PaymentStatus.ok)
      case "CANCELLED_OR_REJECTED" => Some(PaymentStatus.cancel)
      case "PROBLEM" => Some(PaymentStatus.error)
      case _ => None
    }
  }

  private def handleVetumaCheckForPayment(payment: Payment, validVetumaCheck: Option[CheckResponse]) = {
    (validVetumaCheck) match {
      case Some(vetumaCheck) =>
        logger.info(s"Got valid Vetuma check $vetumaCheck")
        db.insertEvent(PaymentEvent(None, payment.id.get, new Date(), vetumaCheck.timestmp, true, vetumaCheck.paymentStatus, vetumaPaymentStatusToPaymentStatus(vetumaCheck.paymentStatus)))
      case None =>
        logger.error(s"Unable to do valid Vetuma check for payment $payment")
        db.insertEvent(PaymentEvent(None, payment.id.get, new Date(), None, false, "UNKNOWN_PAYMENT", None))
    }
  }

  private def isValidVetumaCheck(vetumaCheck: CheckResponse) = !"ERROR".equals(vetumaCheck.status)

  private def asSimpleRunnable(f: () => Unit) = new Runnable() {
    override def run() {
      f()
    }
  }
}
