package fi.vm.sade.hakuperusteet.domain

import fi.vm.sade.hakuperusteet.domain.Hakukausi.Hakukausi

case class ApplicationObject(id: Option[Int], personOid: String, hakukohdeOid: String, hakuOid: String, educationLevel: String, educationCountry: String)

case class ApplicationObjectWithHakukausi(id: Option[Int], personOid: String, hakukohdeOid: String, hakuOid: String, educationLevel: String, educationCountry: String, hakukausi: Hakukausi)