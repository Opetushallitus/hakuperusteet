package fi.vm.sade.hakuperusteet.vetuma

import java.text.SimpleDateFormat
import java.util.Date

import org.apache.commons.codec.digest.DigestUtils

case class VetumaQuery(sharedSecret: String, ap: String, rcvid: String, timestamp: Date, language: String, returnUrl: String, cancelUrl: String,
                       errorUrl: String, solist: String, trid: Option[String], appid: String, paymCallId: String) {

  val dtf = new SimpleDateFormat("yyyyMMddHHmmssSSS")
  val so = ""
  val `type` = "PAYMENT"
  val au = "CHECK"
  val extradata = ""

  private def formatTime = dtf.format(timestamp)

  def plainText =
    s"$rcvid&$appid&$formatTime&$so&$solist&${`type`}&$au&$language&$returnUrl&$cancelUrl&$errorUrl&" +
      s"$ap&$paymCallId$optionalTrid&$sharedSecret&"

  private def optionalTrid = trid.map(t => s"&$t").getOrElse("")

  private def optionalTridParam = trid.map(t=> Map("TRID" -> t)).getOrElse(Map())

  def mac = DigestUtils.sha256Hex(plainText).toUpperCase

  def toParams = Map("RCVID" -> rcvid, "APPID" -> appid, "TIMESTMP" -> formatTime, "SO" -> so, "SOLIST" -> solist,
    "TYPE" -> `type`, "AU" -> au, "LG" -> language, "RETURL" -> returnUrl, "CANURL" -> cancelUrl, "ERRURL" -> errorUrl,
    "AP" -> ap,
    "MAC" -> mac,
    "PAYM_CALL_ID" -> paymCallId) ++ optionalTridParam
}
