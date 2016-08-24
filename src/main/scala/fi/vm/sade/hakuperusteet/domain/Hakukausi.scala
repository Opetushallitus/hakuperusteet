package fi.vm.sade.hakuperusteet.domain

object Hakukausi extends Enumeration {
  type Hakukausi = Value
  val s2016, k2017 = Value

  def koulutuksenAlkamiskausiToHakukausi(koulutuksenAlkamisVuosi:Int, koulutuksenAlkamiskausiUri:String): Hakukausi = {
    (koulutuksenAlkamisVuosi, koulutuksenAlkamiskausiUri) match {
      case (2016, "kausi_s#1") => s2016
      case (2017, "kausi_k#1") => k2017
    }
  }
}
