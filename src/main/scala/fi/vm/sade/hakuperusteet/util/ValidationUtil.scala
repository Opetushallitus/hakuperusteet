package fi.vm.sade.hakuperusteet.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

import fi.vm.sade.hakuperusteet.domain.IDPEntityId
import fi.vm.sade.hakuperusteet.domain.IDPEntityId.IDPEntityId
import fi.vm.sade.utils.validator.InputNameValidator

import scala.util.Try
import scalaz._
import scalaz.syntax.validation._

trait ValidationUtil {
  type ValidationResult[A] = ValidationNel[String, A]
  type Params = Map[String, String]

  def parseExists(key: String)(params: Params) = params.get(key).map(_.successNel)
    .getOrElse(s"Parameter $key does not exist".failureNel)

  def parseNonEmpty(key: String)(params: Params) = parseExists(key)(params)
    .flatMap(a => if (a.nonEmpty) a.successNel else s"Parameter $key is empty".failureNel)

  def parseValidName(key: String)(params: Params) = parseNonEmpty(key)(params)
    .flatMap(a => InputNameValidator.validate(a))

  def parseOptional(key: String)(params: Params) = params.get(key) match { case e => e.successNel }

  def parseOptionalInt(key: String)(params: Params) = (params.get(key): @unchecked) match {  case None => None.successNel
  case Some(i) => Try(Option(i).map(_.toInt).successNel).recover{
    case e => e.getMessage.failureNel
  }.get}

  def parseLocalDate(input: String): ValidationResult[LocalDate] =
    Try(LocalDate.parse(input, DateTimeFormatter.ofPattern("ddMMyyyy")).successNel).recover {
      case e => e.getMessage.failureNel
    }.get

  def personalIdToDate(id: String): ValidationResult[LocalDate] = {
    val daysAndMonths = id.substring(0, 4) // ddmmyy
    val decadeAndYear = id.substring(4, 6)
    val century = id.substring(6, 7).toUpperCase match {
      case "+" => 18
      case "-" => 19
      case "A" => 20
    } // + = 1800, - = 1900, A = 2000
    parseLocalDate(daysAndMonths + century + decadeAndYear)
  }

  case class Identification(birthDate: LocalDate, personalId: Option[String])

  def parseIdentification(params: Params): ValidationResult[Identification] =
    (params.get("birthDate"), params.get("personId")) match {
      case (Some(b), None) =>
        parseLocalDate(b) match {
          case scalaz.Success(birthDateParsed) => Identification(birthDateParsed, None).successNel
          case scalaz.Failure(e) => s"invalid birthDate $b".failureNel
        }
      case (None, Some(p)) =>
        personalIdToDate(p) match {
          case scalaz.Success(birthDateParsed) => Identification(birthDateParsed, Some(p)).successNel
          case scalaz.Failure(e) => s"invalid birthDate".failureNel
        }
      case (None, None) => s"Requires either birthDate or personId, got neither".failureNel
      case (Some(_), Some(_)) => s"Requires either birthDate or personId, got both".failureNel
    }

  def parseIDPEntityId(params: Params): ValidationResult[IDPEntityId] = parseNonEmpty("idpentityid")(params) flatMap
    (name => Try(IDPEntityId withName name) map (_.successNel) getOrElse (s"invalid idpentityid $name".failureNel))
}
