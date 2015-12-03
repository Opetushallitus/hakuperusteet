package fi.vm.sade.hakuperusteet.admin

import java.io.Serializable
import java.time.format.{DateTimeFormatterBuilder, DateTimeFormatter}
import java.time.temporal.ChronoField
import java.util.Date
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit._

import com.google.api.client.util.IOUtils
import com.netaporter.uri.Uri._
import com.netaporter.uri.config.UriConfig
import com.netaporter.uri.parsing.DefaultUriParser
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import fi.vm.sade.hakuperusteet.db.HakuperusteetDatabase
import fi.vm.sade.hakuperusteet.domain.{PaymentEvent, Payment}
import fi.vm.sade.hakuperusteet.vetuma.{VetumaGuessMac, Vetuma}
import org.apache.http.client.fluent.{Form, Request}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

private case class VetumaCheck(paymentStatus: String,
                               status: String,
                               mac: Option[String],
                               timestmp: Option[Date],
                               payId: Option[String],
                               paid: Option[String],
                               rcvid: Option[String],
                               returl: Option[String],
                               canurl: Option[String],
                               errurl: Option[String],
                               lg: Option[String],
                               so: Option[String],
                               paymentCurrency: Option[String],
                               paymentAmount: Option[String])

class PaymentSynchronization(config: Config, db: HakuperusteetDatabase, vetumaGuessMac: VetumaGuessMac) extends LazyLogging {
  val vetumaQueryHost = s"${config.getString("vetuma.host")}Query"
  val vetumaTimestampFormatter = DateTimeFormat.forPattern("yyyyMMddHHmmssSSS") // java time API had bug so did this with jodatime (https://bugs.openjdk.java.net/browse/JDK-8031085) fix comes in Java 9
  val scheduler = Executors.newScheduledThreadPool(1)

  def start = scheduler.scheduleWithFixedDelay(checkPaymentSynchronizations, 1, config.getDuration("admin.synchronization.interval", SECONDS), SECONDS)

  def checkPaymentSynchronizations = asSimpleRunnable { () =>
    try {
    db.findUnchekedPayments.headOption.flatMap(db.findPayment) match {
      case Some(payment: Payment) =>
        (payment.mac) match {
          case Some(mac) => handleWithMacCase(payment)
          case None => handleNoMacCase(payment)
        }
      case _ =>
        logger.debug("All payments up to date with Vetuma.")
    }
    } catch {
      case e => println(e)
    }
  }

  private def handleWithMacCase(paymentWithMac: Payment) = {
    handleVetumaCheckForPayment(paymentWithMac, doVetumaCheck(paymentWithMac, paymentWithMac.mac.get).filter(isValidVetumaCheck))
  }

  private def handleNoMacCase(paymentWithNoMac: Payment) = {
    val macCandidates = vetumaGuessMac.paymentToMacCandidates(paymentWithNoMac)
    if (macCandidates.isEmpty) {
      logger.warn(s"Could not guess MAC for $paymentWithNoMac")
      db.insertEvent(PaymentEvent(None, paymentWithNoMac.id.get, new Date(), None, false, "UNKNOWN_PAYMENT"))
    }
    handleVetumaCheckForPayment(paymentWithNoMac, macCandidates.flatMap(macCandidate => doVetumaCheck(paymentWithNoMac, macCandidate)).find(isValidVetumaCheck))
  }

  private def handleVetumaCheckForPayment(payment: Payment, validVetumaCheck: Option[VetumaCheck]) = {
    (validVetumaCheck) match {
      case Some(vetumaCheck) =>
        logger.info(s"Got valid Vetuma check $vetumaCheck")
        db.insertEvent(PaymentEvent(None, payment.id.get, new Date(), vetumaCheck.timestmp, true, vetumaCheck.paymentStatus))
      case None =>
        logger.error(s"Unable to do valid Vetuma check for payment $payment")
        db.insertEvent(PaymentEvent(None, payment.id.get, new Date(), None, false, "UNKNOWN_PAYMENT"))
    }
  }

  private def isValidVetumaCheck(vetumaCheck: VetumaCheck) = !"UNKNOWN_PAYMENT".equals(vetumaCheck.paymentStatus)

  private def doVetumaCheck(paymentWithOrWithoutMac: Payment, mac: String): Option[VetumaCheck] = doVetumaCheck(paymentWithOrWithoutMac.copy(mac = Some(mac)))

  private def doVetumaCheck(paymentWithMac: Payment): Option[VetumaCheck] = {
    val queryParams = Vetuma.query(config, paymentWithMac)

    val formBuilder = queryParams.foldLeft(Form.form())((form, entry) => {
      val (key, value) = entry
      form.add(key, value)
    })
    val vetumaQueryResponse = Request.Post(vetumaQueryHost).addHeader("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
      .bodyForm(formBuilder.build()).execute().returnResponse()
    try {
      val content = scala.io.Source.fromInputStream(vetumaQueryResponse.getEntity.getContent).mkString
      // Vetuma doesnt return duplicates so this is safe, spec page 43 (55)
      val query = parse(s"?$content").query.params.map(entry => {
        val (key, value) = entry
        key -> value
      }).toMap
      val paymentStatus = query.getOrElse("PAYM_STATUS", None).getOrElse("")
      val status = query.getOrElse("STATUS", None).getOrElse("")
      Some(VetumaCheck(paymentStatus,
        status,
        query.get("MAC").flatten,
        query.get("TIMESTMP").flatten.map(vetumaTimestampFormatter.parseDateTime).map(_.toDate),
        query.get("PAYID").flatten,
        query.get("PAID").flatten,
        query.get("RCVID").flatten,
        query.get("RETURL").flatten,
        query.get("CANURL").flatten,
        query.get("ERRURL").flatten,
        query.get("LG").flatten,
        query.get("SO").flatten,
        query.get("PAYM_AMOUNT").flatten,
        query.get("PAYM_CURRENCY").flatten))
    } catch {
      case e =>
        logger.error(s"Unable to read Vetuma answer for $paymentWithMac", e)
        None
    }
  }

  private def asSimpleRunnable(f: () => Unit) = new Runnable() {
    override def run() {
      f()
    }
  }
}
