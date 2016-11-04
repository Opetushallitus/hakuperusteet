package fi.vm.sade.hakuperusteet.validation

import fi.vm.sade.hakuperusteet.domain.ApplicationObject
import fi.vm.sade.hakuperusteet.koodisto.{Countries, Educations}
import fi.vm.sade.hakuperusteet.util.ValidationUtil

import scalaz._
import scalaz.Validation.FlatMap._
import scalaz.syntax.validation._

case class ApplicationObjectValidator(countries: Countries, educations: Educations) extends ValidationUtil {

  def parseApplicationObjectWithoutPersonOid(params: Params): ValidationResult[(String) => ApplicationObject] = {
    Apply[ValidationResult].apply4(
      parseNonEmpty("hakukohdeOid")(params),
      parseNonEmpty("hakuOid")(params),
      parseExists("educationLevel")(params).flatMap(validateEducationLevel),
      parseExists("educationCountry")(params).flatMap(validateCountry)
    ) { (hakukohdeOid, hakuOid, educationLevel, educationCountry) =>
      ApplicationObject(None, _:String, hakukohdeOid, hakuOid, educationLevel, educationCountry)
    }
  }

  def parseApplicationObject(params: Params): ValidationResult[ApplicationObject] = {
    Apply[ValidationResult].apply6(
      parseOptionalInt("id")(params),
      parseNonEmpty("hakukohdeOid")(params),
      parseNonEmpty("personOid")(params),
      parseNonEmpty("hakuOid")(params),
      parseExists("educationLevel")(params).flatMap(validateEducationLevel),
      parseExists("educationCountry")(params).flatMap(validateCountry)
    ) { (id, hakukohdeOid, personOid, hakuOid, educationLevel, educationCountry) =>
      ApplicationObject(id, personOid, hakukohdeOid, hakuOid, educationLevel, educationCountry)
    }
  }

  private def validateCountry(nationality: String): ValidationResult[String] =
    if (countries.countries.map(_.id).contains(nationality) || nationality.isEmpty) nationality.successNel
    else s"unknown country $nationality".failureNel

  private def validateEducationLevel(educationLevel: String): ValidationResult[String] =
    if (educations.educations.map(_.id).contains(educationLevel) || educationLevel.isEmpty) educationLevel.successNel
    else s"unknown educationLevel $educationLevel".failureNel
}
