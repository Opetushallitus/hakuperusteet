package fi.vm.sade.hakuperusteet.domain

object Hakumaksukausi extends Enumeration {
  type Hakumaksukausi = Value
  val s2016, k2017 = Value

  def koulutuksenAlkamiskausiToHakumaksukausi(koulutuksenAlkamisVuosi:Int, koulutuksenAlkamiskausiUri:String): Hakumaksukausi = {
    (koulutuksenAlkamisVuosi, koulutuksenAlkamiskausiUri) match {
      case (2016, "kausi_s#1") => s2016
      case (2017, "kausi_k#1") => k2017
    }
  }
}
