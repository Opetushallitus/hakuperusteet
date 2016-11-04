package fi.vm.sade.hakuperusteet.domain

import fi.vm.sade.hakuperusteet.domain.Hakumaksukausi.Hakumaksukausi

case class ApplicationObject(id: Option[Int], personOid: String, hakukohdeOid: String, hakuOid: String, educationLevel: String, educationCountry: String)

case class ApplicationObjectWithHakumaksukausi(id: Option[Int], personOid: String, hakukohdeOid: String, hakuOid: String, educationLevel: String, educationCountry: String, hakumaksukausi: Option[Hakumaksukausi])