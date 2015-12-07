package fi.vm.sade.hakuperusteet.admin

import java.io.{File, Serializable}
import java.time.format.{DateTimeFormatterBuilder, DateTimeFormatter}
import java.time.temporal.ChronoField
import java.util.Date
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit._

import com.google.api.client.util.IOUtils
import com.netaporter.uri.Uri._
import com.netaporter.uri.config.UriConfig
import com.netaporter.uri.parsing.DefaultUriParser
import com.typesafe.config.{ConfigFactory, Config}
import com.typesafe.scalalogging.LazyLogging
import fi.vm.sade.hakuperusteet.db.HakuperusteetDatabase
import fi.vm.sade.hakuperusteet.domain.{PaymentStatus, PaymentEvent, Payment}
import fi.vm.sade.hakuperusteet.vetuma.{VetumaCheck, CheckResponse, VetumaGuessMac, Vetuma}
import org.apache.http.client.fluent.{Form, Request}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat



class PaymentSynchronization(config: Config, db: HakuperusteetDatabase, vetumaGuessMac: VetumaGuessMac) extends LazyLogging {
  val vetumaCheck = new VetumaCheck(config, "PAYMENT-APP2", "P")
  val vetumaQueryHost = s"${config.getString("vetuma.host")}Query"

  val scheduler = Executors.newScheduledThreadPool(1)

  def start = scheduler.scheduleWithFixedDelay(checkPaymentSynchronizations, 1, config.getDuration("admin.synchronization.interval", SECONDS), SECONDS)

  def checkPaymentSynchronizations = asSimpleRunnable { () =>
    db.findUnchekedPayments.foreach(id => db.findPayment(id) match {
      case Some(payment: Payment) =>
        try {
        val u = db.findUserByOid(payment.personOid) match {
          case Some(u) =>
            (payment.mac) match {
              case Some(mac) => handleWithMacCase(payment, u.uiLang)
              case None => handleNoMacCase(payment, u.uiLang)
            }
          case None =>
            logger.error("No user found")
        }

        } catch {
          case e => logger.error(s"$e")
        }
      case _ =>
        logger.debug("All payments up to date with Vetuma.")
    })
  }

  private def handleWithMacCase(paymentWithMac: Payment, language: String) = {
    handleVetumaCheckForPayment(paymentWithMac, doVetumaCheck(paymentWithMac, paymentWithMac.mac.get, language).filter(isValidVetumaCheck))
  }

  private def handleNoMacCase(paymentWithNoMac: Payment, language: String) = {
    val macCandidates = vetumaGuessMac.paymentToMacCandidates(paymentWithNoMac)
    if (macCandidates.isEmpty) {
      logger.warn(s"Could not guess MAC for $paymentWithNoMac")
      db.insertEvent(PaymentEvent(None, paymentWithNoMac.id.get, new Date(), None, false, "UNKNOWN_PAYMENT"))
    }
    handleVetumaCheckForPayment(paymentWithNoMac, macCandidates.flatMap(macCandidate => doVetumaCheck(paymentWithNoMac, macCandidate, language)).find(isValidVetumaCheck))
  }

  private def handleVetumaCheckForPayment(payment: Payment, validVetumaCheck: Option[CheckResponse]) = {
    (validVetumaCheck) match {
      case Some(vetumaCheck) =>
        logger.info(s"Got valid Vetuma check $vetumaCheck")
        db.insertEvent(PaymentEvent(None, payment.id.get, new Date(), vetumaCheck.timestmp, true, vetumaCheck.paymentStatus))
      case None =>
        logger.error(s"Unable to do valid Vetuma check for payment $payment")
        db.insertEvent(PaymentEvent(None, payment.id.get, new Date(), None, false, "UNKNOWN_PAYMENT"))
    }
  }

  private def isValidVetumaCheck(vetumaCheck: CheckResponse) = !"UNKNOWN_PAYMENT".equals(vetumaCheck.paymentStatus)

  private def doVetumaCheck(paymentWithOrWithoutMac: Payment, mac: String, language: String): Option[CheckResponse] = doVetumaCheck((paymentWithOrWithoutMac.copy(mac = Some(mac)), language))

  private def doVetumaCheck(paymentWithMacAndLanguage: (Payment, String)): Option[CheckResponse] = {
    val (paymentWithMac, language) = paymentWithMacAndLanguage
    val ok = "https://localhost/ShowPayment.asp"
    val cancel = "https://localhost/ShowCancel.asp"
    val error = "https://localhost/ShowError.asp"
    vetumaCheck.doVetumaCheck(paymentWithMac.paymCallId, new Date(), language, ok, cancel, error, None)
  }

  private def asSimpleRunnable(f: () => Unit) = new Runnable() {
    override def run() {
      f()
    }
  }
}
