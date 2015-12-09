package fi.vm.sade.hakuperusteet.domain

import java.time.{ZoneId, LocalDate}
import java.util.Date
import fi.vm.sade.hakuperusteet.personIdDateFormatter
import org.slf4j.LoggerFactory

object Users {

  def generateUsers = {
    val userData = (firstNameGenderAndPersonIDs zip lastNames) zipWithIndex

    userData.map{ case (((name, gender, personId), lastName), i) =>
      User(None, Some(personOidFromIndex(i)), s"${name}.${lastName}@example.com".toLowerCase, Some(name), Some(lastName), Some(birthDate), Option(personId), Google, Some(gender), Some("AB"), Some("004"), "en") }
  }

  private def personOidFromIndex(index: Int) = f"1.2.246.562.24.${index + 1000}%011d"

  private val MIES = "1"
  private val NAINEN = "2"

  private val firstNameGenderAndPersonIDs = List(
    ("Anni", NAINEN, "261095-910P"), ("Ossi", MIES, "261095-939M"),
    ("Pentti", MIES, "261095-977V"), ("Ritva",NAINEN, "261095-904H"),
    ("Ilja", MIES, "261095-933E"), ("Pirjo", NAINEN, "261095-962C"),
    ("Kalevi", MIES, "261095-9854"), ("Marja", NAINEN, "261095-9843"),
    ("Jouko", MIES, "301195-9756"))

  private val lastNames = List("Annilainen", "Ossilainen", "Penttiläinen", "Ritvanen", "Iljanen", "Simonen", "Kalevinen", "Marjanen","Maksuton")

  private def birthDate = Date.from(LocalDate.from(personIdDateFormatter.parse("261095")).atStartOfDay(ZoneId.systemDefault()).toInstant())
}
