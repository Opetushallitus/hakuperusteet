package fi.vm.sade.hakuperusteet.domain

import java.time.{ZoneId, LocalDate}
import java.util.Date

import scala.util.Random

object Payments {

  val rnd = Random

  def generateNumSeq = "%09d".format(Math.abs(rnd.nextInt()))

  def generatePayments(user:User) = {
    Range(1,2).map(value =>
      Payment(None, user.personOid.get,
        Date.from(
          LocalDate.now().minusYears(value).atStartOfDay(ZoneId.systemDefault()).toInstant()),
        generateNumSeq,generateNumSeq,generateNumSeq,PaymentStatus.ok, None, None))
  }
}
