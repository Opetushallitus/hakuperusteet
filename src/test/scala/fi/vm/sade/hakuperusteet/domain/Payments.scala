package fi.vm.sade.hakuperusteet.domain

import java.time.{ZoneId, LocalDate}
import java.util.Date

import org.slf4j.LoggerFactory
import fi.vm.sade.hakuperusteet.domain.AbstractUser.User
import scala.util.Random

object Payments {
  val logger = LoggerFactory.getLogger(this.getClass)

  val rnd = Random

  def generateNumSeq = "%09d".format(Math.abs(rnd.nextInt()))

  def generatePayments(user:User, noPaymentNames: List[String], noK2017Names: List[String]) = {
    if(noPaymentNames.exists(name => name.equals(user.lastName.get))) {
      List()
    } else {
      val s2016 = Range(1, 4).map(value =>
        Payment(None, user.personOid.get,
          Date.from(
            LocalDate.now().minusYears(value).atStartOfDay(ZoneId.systemDefault()).toInstant()),
          generateNumSeq, generateNumSeq, generateNumSeq, PaymentStatus.ok, Hakumaksukausi.s2016, None))
      if(noK2017Names.exists(name => name.equals(user.lastName.get))) {
        s2016
      } else {
        s2016 ++ Range(1, 2).map(value =>
          Payment(None, user.personOid.get,
            Date.from(
              LocalDate.now().minusYears(value).atStartOfDay(ZoneId.systemDefault()).toInstant()),
            generateNumSeq, generateNumSeq, generateNumSeq, PaymentStatus.cancel, Hakumaksukausi.k2017, None))
      }
    }
  }
}
