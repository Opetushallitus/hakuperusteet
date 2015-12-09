package fi.vm.sade.hakuperusteet.validation

import java.time.LocalDate

import fi.vm.sade.hakuperusteet.domain.{OppijaToken, User}
import fi.vm.sade.hakuperusteet.koodisto.{Countries, Languages, SimplifiedCode, SimplifiedLangValue}
import org.scalatest.{Assertions, FlatSpec, Matchers}

import scalaz.{Failure, NonEmptyList}

class UserValidatorSpec extends FlatSpec with Matchers {
  val codes: List[SimplifiedCode] = List(SimplifiedCode("fin", List(SimplifiedLangValue("fin", "Suomi"))))
  val validator = UserValidator(Countries(codes, List()), Languages(codes))
  val testUser = Map(
    "gender" -> "1",
    "firstName" -> "Etunimi",
    "lastName" -> "Sukunimi",
    "nativeLanguage" -> "fin",
    "nationality" -> "fin",
    "personId" -> "011295-923l",
    "birthDate" -> "20101990")

  it should "check all missing parameters for partial user" in {
    val validator = UserValidator(Countries(List(), List()), Languages(List()))
    val result = validator.parseUserDataWithoutEmailAndIdpentityid(Map())
    result shouldEqual Failure(NonEmptyList(
      "Parameter firstName does not exist",
      "Parameter lastName does not exist",
      "Requires either birthDate or personId, got neither",
      "Parameter gender does not exist",
      "Parameter nativeLanguage does not exist",
      "Parameter nationality does not exist"))
  }

  it should "pass when using valid parameters and birth date for partial user" in {
    val result = validator.parseUserDataWithoutEmailAndIdpentityid(testUser - "personId").getOrElse(Assertions.fail())
    val user: User = result("", OppijaToken.toString, "")

    user.birthDate.get.asInstanceOf[java.sql.Date].toLocalDate shouldEqual LocalDate.of(1990, 10, 20)
    user.personId shouldEqual None
  }

  it should "pass and parse birthdate when using valid input and personal identification code" in {
    val result = validator.parseUserDataWithoutEmailAndIdpentityid(testUser - "birthDate").getOrElse(Assertions.fail())
    val user: User = result("", OppijaToken.toString, "")

    user.birthDate.get.asInstanceOf[java.sql.Date].toLocalDate shouldEqual LocalDate.of(1995, 12, 1)
    user.personId shouldEqual Some("011295-923L")
  }

  it should "fail when passing both personal identity code and birthdate for partial user" in {
    validator.parseUserDataWithoutEmailAndIdpentityid(testUser) shouldEqual
      Failure(NonEmptyList("Requires either birthDate or personId, got both"))
  }

  it should "fail when passing neither personal identity nor birthdate for partial user" in {
    validator.parseUserDataWithoutEmailAndIdpentityid(testUser - "birthDate" - "personId") shouldEqual
      Failure(NonEmptyList("Requires either birthDate or personId, got neither"))
  }

  it should "fail when using malformed personId for partial user" in {
    validator.parseUserDataWithoutEmailAndIdpentityid(testUser - "birthDate" + ("personId" -> "011295-9694")) shouldEqual
      Failure(NonEmptyList("invalid personal identity code 011295-9694 - [checksum character 4 is invalid]"))
  }

  it should "fail when using malformed birth date for partial user" in {
    validator.parseUserDataWithoutEmailAndIdpentityid(testUser - "personId" + ("birthDate" -> "20301990")) shouldEqual
      Failure(NonEmptyList("invalid birthDate 20301990"))
  }

  it should "check all missing parameters" in {
    val validator = UserValidator(Countries(List(), List()), Languages(List()))
    val result = validator.parseUserData(Map())
    result shouldEqual Failure(NonEmptyList(
      "Parameter email does not exist",
      "Parameter firstName does not exist",
      "Parameter lastName does not exist",
      "Requires either birthDate or personId, got neither",
      "Parameter idpentityid does not exist",
      "Parameter gender does not exist",
      "Parameter nativeLanguage does not exist",
      "Parameter nationality does not exist",
      "Parameter uiLang does not exist"))
  }
}
