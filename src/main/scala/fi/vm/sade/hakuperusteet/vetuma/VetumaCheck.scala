package fi.vm.sade.hakuperusteet.vetuma

import java.util.Date

import com.netaporter.uri.Uri._
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import org.apache.http.client.fluent.{Request, Form}
import org.joda.time.format.DateTimeFormat

case class CheckResponse(paymentStatus: String,
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

class VetumaCheck(val config: Config, val appid: String, val so: String) extends LazyLogging {
  val vetumaQueryHost = s"${config.getString("vetuma.host")}Query"
  val vetumaTimestampFormatter = DateTimeFormat.forPattern("yyyyMMddHHmmssSSS") // java time API had bug so did this with jodatime (https://bugs.openjdk.java.net/browse/JDK-8031085) fix comes in Java 9

  def doVetumaCheck(paymCallId: String, timestamp: Date, language: String, trid: Option[String]): Option[CheckResponse] = {
    // These dont matter, using same values as in Vetuma example CHECK query
    val hrefOk = "https://localhost/ShowPayment.asp"
    val hrefCancel = "https://localhost/ShowCancel.asp"
    val hrefError = "https://localhost/ShowError.asp"
    val queryParams = Vetuma.query(config, paymCallId, language, hrefOk, hrefCancel, hrefError, timestamp, so, trid, appid).toParams

    val formBuilder = queryParams.foldLeft(Form.form())((form, entry) => {
      val (key, value) = entry
      form.add(key, value)
    })
    val vetumaQueryResponse = Request.Post(vetumaQueryHost).addHeader("Content-Type", "application/x-www-form-urlencoded;charset=utf-8")
      .bodyForm(formBuilder.build()).execute().returnResponse()
    try {
      val content = scala.io.Source.fromInputStream(vetumaQueryResponse.getEntity.getContent).mkString
      val query = parse(s"?$content").query.params.map(entry => {
        val (key, value) = entry
        key -> value
      }).toMap
      val paymentStatus = query.getOrElse("PAYM_STATUS", None).getOrElse("")
      val status = query.getOrElse("STATUS", None).getOrElse("")
      Some(CheckResponse(paymentStatus,
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
      case e: Throwable =>
        logger.error(s"Unable to read Vetuma answer for payment with id $paymCallId", e)
        None
    }
  }
}
