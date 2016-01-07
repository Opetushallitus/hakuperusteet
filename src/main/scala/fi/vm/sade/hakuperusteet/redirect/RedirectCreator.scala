package fi.vm.sade.hakuperusteet.redirect

import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import fi.vm.sade.hakuperusteet.domain.AbstractUser._
import fi.vm.sade.hakuperusteet.domain.ApplicationObject
import fi.vm.sade.hakuperusteet.rsa.RSASigner

object RedirectCreator {

  def generateParamMap(signer: RSASigner, userData: User, educationForThisHakukohde: ApplicationObject, shouldPay: Boolean, hasPaid: Boolean, admin: Boolean = false) = {
    val seq = paramSequence(userData, shouldPay, hasPaid, educationForThisHakukohde, admin)
    val signature = signer.signData(seq.map(_._2).mkString(""))
    (seq.toList ++ List(("signature", signature), ("lang", userData.uiLang))).toMap
  }

  private def isAdminRequest(admin: Boolean) =
    if (admin)
      Seq("admin" -> "true")
    else
      Seq()

  def paramSequence(u: User, shouldPay: Boolean, hasPaid: Boolean, e: ApplicationObject, admin: Boolean) =
    Seq(
      ("personOid", u.personOid.get),
      ("email", u.email),
      ("firstName", u.firstName.get),
      ("lastName", u.lastName.get),
      ("birthDate", new SimpleDateFormat("ddMMyyyy").format(u.birthDate.get)),
      ("personId", u.personId.getOrElse("")),
      ("gender", u.gender.get),
      ("nationality", u.nationality.get),
      ("nativeLanguage", u.nativeLanguage.get),
      ("hakukohdeOid", e.hakukohdeOid),
      ("educationLevel", e.educationLevel),
      ("educationCountry", e.educationCountry),
      ("shouldPay", shouldPay.toString),
      ("hasPaid", hasPaid.toString),
      ("created", new Date().toInstant.getEpochSecond.toString)
    ) ++ isAdminRequest(admin)

  def generatePostBody(params: Map[String, String]) = params.map { case (k, v) => k + "=" + URLEncoder.encode(v, "UTF-8") }.mkString("&")
}
