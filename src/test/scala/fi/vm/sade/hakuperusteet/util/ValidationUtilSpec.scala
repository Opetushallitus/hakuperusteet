package fi.vm.sade.hakuperusteet.util

import java.time.LocalDate

import org.scalatest.{FlatSpec, Matchers}

class Util extends ValidationUtil

class ValidationUtilSpec extends FlatSpec with Matchers with ValidationUtil {
  it should "Get translated text" in {
    val person1800 = "161242+955M"
    val person1900 = "021295-989C"
    val person2000 = "300401A9715"
    val person2000lowercase = "300401a9715"
    personalIdToDate(person1800).toList.head shouldEqual LocalDate.of(1842, 12, 16)
    personalIdToDate(person1900).toList.head shouldEqual LocalDate.of(1995, 12, 2)
    personalIdToDate(person2000).toList.head shouldEqual LocalDate.of(2001, 4, 30)
    personalIdToDate(person2000lowercase).toList.head shouldEqual LocalDate.of(2001, 4, 30)
  }
}
