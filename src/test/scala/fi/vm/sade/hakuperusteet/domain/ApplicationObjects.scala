package fi.vm.sade.hakuperusteet.domain

import fi.vm.sade.hakuperusteet.domain.AbstractUser.User

object ApplicationObjects {
  private def maksumuuriHakuOid = "1.2.246.562.29.80171652938"
  private def maksumuuriHakukohdeOid = List(("1.2.246.562.20.69046715533","102"), ("1.2.246.562.20.31077988074", "100"))
  private def tunnistusHakuOid = "1.2.246.562.29.80171652951"
  private def tunnistusHakukohdeOid ="1.2.246.562.20.69046715555"

  def generateApplicationObject(u: User): List[ApplicationObject] = {
    Range(0,2).map(value => maksumuuriHakukohdeOid(value) match {case(hakukohde,baseEducation) => ApplicationObject(None, u.personOid.get, hakukohde, maksumuuriHakuOid, baseEducation, "008")}).toList :+
      ApplicationObject(None, u.personOid.get, tunnistusHakukohdeOid, tunnistusHakuOid, "", "")
  }
}
