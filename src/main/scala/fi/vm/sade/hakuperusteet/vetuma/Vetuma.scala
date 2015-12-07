package fi.vm.sade.hakuperusteet.vetuma

import java.text.SimpleDateFormat
import java.util.Date

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import fi.vm.sade.hakuperusteet.domain.Payment
import org.apache.commons.codec.digest.DigestUtils



case class Vetuma(sharedSecret: String, ap: String, rcvid: String, timestamp: Date, language: String, returnUrl: String, cancelUrl: String,
                     errorUrl: String, appName: String, amount: String, ref: String, orderNumber: String,
                     msgBuyer: String, msgSeller: String, msgForm: String, paymCallId: String) {

  val dtf = new SimpleDateFormat("yyyyMMddHHmmssSSS")
  val appid = "PAYMENT-APP2"
  val so = ""
  val solist = "P,L"
  val `type` = "PAYMENT"
  val au = "PAY"

  private def formatTime = dtf.format(timestamp)

  private def plainText =
    s"$rcvid&$appid&$formatTime&$so&$solist&${`type`}&$au&$language&$returnUrl&$cancelUrl&$errorUrl&" +
    s"$ap&$appName&$amount&$ref&$orderNumber&$msgBuyer&$msgSeller&$msgForm&$paymCallId&$sharedSecret&"

  def mac = DigestUtils.sha256Hex(plainText).toUpperCase

  def toParams = Map("RCVID" -> rcvid, "APPID" -> appid, "TIMESTMP" -> formatTime, "SO" -> so, "SOLIST" -> solist,
    "TYPE" -> `type`, "AU" -> au, "LG" -> language, "RETURL" -> returnUrl, "CANURL" -> cancelUrl, "ERRURL" -> errorUrl,
    "AP" -> ap, "APPNAME" -> appName, "AM" -> amount, "REF" -> ref, "ORDNR" -> orderNumber, "MAC" -> mac,
    "MSGBUYER" -> msgBuyer, "MSGSELLER" -> msgSeller, "MSGFORM" -> msgForm, "PAYM_CALL_ID" -> paymCallId)
}

object Vetuma extends LazyLogging {
  def query(config: Config, paymCallId: String, language: String, hrefOk: String, hrefCancel: String, hrefError: String, timestamp: Date, solist: String, trid: Option[String], appid: String) = {
    VetumaQuery(
      config.getString("vetuma.shared.secret"),
      config.getString("vetuma.shared.ap"),
      config.getString("vetuma.shared.rcvid"),
      timestamp,
      language,
      hrefOk,
      hrefCancel,
      hrefError,
      solist,
      trid,
      appid,
      paymCallId
    )
  }

  def apply(config: Config, payment: Payment, language: String, href: String, params: String): Vetuma = {
    val q = s"?href=$href$params"
    val returnUrl = s"$href${config.getString("vetuma.success.url")}$q"
    val cancelUrl = s"$href${config.getString("vetuma.cancel.url")}$q"
    val errorUrl = s"$href${config.getString("vetuma.error.url")}$q"
    Vetuma(
      config.getString("vetuma.shared.secret"),
      config.getString("vetuma.shared.ap"),
      config.getString("vetuma.shared.rcvid"),
      payment.timestamp,
      language,
      returnUrl,
      cancelUrl,
      errorUrl,
      config.getString("vetuma.app.name"),
      config.getString("vetuma.amount"),
      payment.reference,
      payment.orderNumber,
      config.getString("vetuma.msg.buyer"),
      config.getString("vetuma.msg.seller"),
      config.getString("vetuma.msg.form"),
      payment.paymCallId
    )
  }

  def verifyReturnMac(sharedSecret: String, orderedParams: List[String], expectedMac: String) = {
    val plainText = (orderedParams ++ List(sharedSecret, "")).mkString("&")
    val calculatedMac = DigestUtils.sha256Hex(plainText).toUpperCase
    val ok = calculatedMac == expectedMac
    if (!ok) {
      logger.warn(s"Invalid Vetuma return mac $calculatedMac - expected mac $expectedMac")
    }
    ok
  }
}