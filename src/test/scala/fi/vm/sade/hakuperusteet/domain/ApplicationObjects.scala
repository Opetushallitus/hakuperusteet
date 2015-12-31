package fi.vm.sade.hakuperusteet.domain

import fi.vm.sade.hakuperusteet.domain.AbstractUser.User

object ApplicationObjects {
  private def hakuOid = "1.2.246.562.29.80171652938"
  private def hakukohdeOid = List(("1.2.246.562.20.69046715533","102"), ("1.2.246.562.20.31077988074", "100"))

  def generateApplicationObject(u: User): List[ApplicationObject] = {
    if (u.lastName.get == "Organisaatioryhmästä") {
      List(ApplicationObject(None, u.personOid.get, "1.2.246.562.20.22222222222", hakuOid, "100", "008"))
    } else {
      Range(0,2).map(value => hakukohdeOid(value) match {case(hakukohde,baseEducation) => ApplicationObject(None, u.personOid.get, hakukohde, hakuOid, baseEducation, "008")}).toList
    }
  }
}
