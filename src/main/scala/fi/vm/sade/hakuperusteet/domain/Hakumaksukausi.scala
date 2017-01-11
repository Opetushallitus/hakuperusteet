package fi.vm.sade.hakuperusteet.domain

import java.util.Calendar

object Hakumaksukausi extends Enumeration {
  type Hakumaksukausi = Value
  val s2016, k2017 = Value

  def syncKoulutus(koulutuksenAlkamisVuosi: Option[Int]):Boolean = {
    val alkamisVuosi: Int = (koulutuksenAlkamisVuosi match{
      case Some(koulutuksenAlkamisVuosi:Int) => koulutuksenAlkamisVuosi
      case _ => 3000
    })

    val year = Calendar.getInstance().get(Calendar.YEAR)
    (year) match {
      case year if year <= alkamisVuosi => true
      case other => false
    }
  }

  def koulutuksenAlkamiskausiToHakumaksukausi(koulutuksenAlkamisVuosi: Option[Int], koulutuksenAlkamiskausiUri: Option[String]): Option[Hakumaksukausi] = {
    (koulutuksenAlkamisVuosi, koulutuksenAlkamiskausiUri) match {
      case (Some(2016), Some("kausi_s#1")) => Some(s2016)
      case (Some(2017), Some("kausi_k#1")) => Some(k2017)
      case other => None
    }
  }

}
