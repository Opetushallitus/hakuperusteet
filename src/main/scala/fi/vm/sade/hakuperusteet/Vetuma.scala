package fi.vm.sade.hakuperusteet

import java.net.URLEncoder

import org.apache.commons.codec.digest.DigestUtils

case class VetumaUrl(host: String, timestamp: String, language: String, returnUrl: String, cancelUrl: String,
                     errorUrl: String, appName: String, amount: String, ref: String, orderNumber: String,
                     msgBuyer: String, msgSeller: String, msgForm: String) {

  val sharedSecret = "TESTIASIAKAS11-873C992B8C4C01EC8355500CAA709B37EA43BC2E591ABF29FEE5EAFE4DCBFA35"
  val rcvid = "TESTIASIAKAS11"
  val appid = "PAYMENT-APP2"
  val so = ""
  val solist = "P,L"
  val `type` = "PAYMENT"
  val au = "PAY"
  val ap = "TESTIASIAKAS1"

  private def plainText =
    s"$rcvid&$appid&$timestamp&$so&$solist&${`type`}&$au&$language&$returnUrl&$cancelUrl&$errorUrl&" +
    s"$ap&$appName&$amount&$ref&$orderNumber&$msgBuyer&$msgSeller&$msgForm&$sharedSecret&"

  private def mac = DigestUtils.sha256Hex(plainText).toUpperCase

  private def query =
    s"RCVID=$rcvid&APPID=$appid&TIMESTMP=$timestamp&SO=$so&SOLIST=$solist&TYPE=${`type`}&AU=$au&LG=$language&" +
    s"RETURL=$returnUrl&CANURL=$cancelUrl&ERRURL=$errorUrl&AP=$ap&APPNAME=${enc(appName)}&AM=$amount&REF=$ref&" +
    s"ORDNR=$orderNumber&MSGBUYER=${enc(msgBuyer)}&MSGSELLER=${enc(msgSeller)}&MSGFORM=${enc(msgForm)}"

  private def enc(value: String) = URLEncoder.encode(value, "UTF-8")

  def toUrl = s"$host?$query&MAC=$mac"
}