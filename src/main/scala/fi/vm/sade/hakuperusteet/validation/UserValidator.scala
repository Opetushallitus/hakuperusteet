package fi.vm.sade.hakuperusteet.validation
import fi.vm.sade.hakuperusteet.domain.AbstractUser._
import fi.vm.sade.hakuperusteet.domain.{AbstractUser, IDPEntityId}
import fi.vm.sade.hakuperusteet.koodisto.{Countries, Languages}
import fi.vm.sade.hakuperusteet.util.ValidationUtil

import scalaz.Apply
import scalaz.syntax.validation._

case class UserValidator(countries: Countries, languages: Languages) extends ValidationUtil {

  def parseUserDataWithoutEmailAndIdpentityid(params: Params): ValidationResult[(String, String, String) => User] = {
    Apply[ValidationResult].apply6(
      parseNonEmpty("firstName")(params),
      parseNonEmpty("lastName")(params),
      parseIdentification(params),
      parseExists("gender")(params).flatMap(validateGender),
      parseExists("nativeLanguage")(params).flatMap(validateNativeLanguage),
      parseExists("nationality")(params).flatMap(validateCountry)
    ) { (firstName, lastName, identification, gender, nativeLanguage, nationality) =>
      (email: String, idpEntityId: String, uiLang: String) => AbstractUser.user(None, None, email, Some(firstName), Some(lastName),
        Some(java.sql.Date.valueOf(identification.birthDate)), identification.personalId, IDPEntityId.withName(idpEntityId),
        Some(gender), Some(nativeLanguage), Some(nationality), uiLang)
    }
  }

  def parseUserData(params: Params): ValidationResult[User] = {
    Apply[ValidationResult].apply11(
      parseOptionalInt("id")(params),
      parseOptional("personOid")(params),
      parseNonEmpty("email")(params),
      parseValidName("firstName")(params),
      parseValidName("lastName")(params),
      parseIdentification(params),
      parseIDPEntityId(params),
      parseExists("gender")(params).flatMap(validateGender),
      parseExists("nativeLanguage")(params).flatMap(validateNativeLanguage),
      parseExists("nationality")(params).flatMap(validateCountry),
      parseExists("uiLang")(params)
    ) { (id, personOid, email, firstName, lastName, identification, idpentityid, gender, nativeLanguage, nationality, uiLang) =>
      AbstractUser.user(id, personOid, email, Some(firstName), Some(lastName), Some(java.sql.Date.valueOf(identification.birthDate)), identification.personalId, idpentityid, Some(gender), Some(nativeLanguage), Some(nationality), uiLang)
    }
  }

  def parseEmailToken(params: Params): ValidationResult[(String, String)] = {
    Apply[ValidationResult].apply2(
      parseNonEmpty("email")(params).flatMap(validateEmail),
      parseExists("hakukohdeOid")(params)) { (email, hakukohdeOid) => (email, hakukohdeOid) }
  }

  private def validateGender(gender: String): ValidationResult[String] =
    if (gender == "1" || gender == "2") gender.successNel
    else  s"gender value $gender is invalid".failureNel

  private def validateNativeLanguage(nativeLanguage: String): ValidationResult[String] =
    if (languages.languages.map(_.id).contains(nativeLanguage)) nativeLanguage.successNel
    else s"unknown nativeLanguage $nativeLanguage".failureNel

  private def validateCountry(nationality: String): ValidationResult[String] =
    if (countries.countries.map(_.id).contains(nationality)) nationality.successNel
    else s"unknown country $nationality".failureNel

  private def validateEmail(email: String): ValidationResult[String] =
    if (!email.isEmpty && email.contains("@") && !email.contains(" ") && !email.contains(",") && !email.contains("\t")) email.successNel
    else s"invalid email $email".failureNel
}
