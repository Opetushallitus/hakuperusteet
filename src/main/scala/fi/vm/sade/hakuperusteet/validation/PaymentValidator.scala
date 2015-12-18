package fi.vm.sade.hakuperusteet.validation

import java.time.{ZoneId, LocalDate}
import java.util.Date

import fi.vm.sade.hakuperusteet.domain.{PaymentStatus, Payment}
import fi.vm.sade.hakuperusteet.util.ValidationUtil
import scala.util.Try
import scalaz._
import scalaz.syntax.applicative._
import scalaz.syntax.validation._

case class PaymentValidator() extends ValidationUtil {

  def parsePaymentWithoutTimestamp(params: Params) = {
    (parseOptionalInt("id")(params)
      |@| parseNonEmpty("personOid")(params)
      |@| parseNonEmpty("reference")(params)
      |@| parseNonEmpty("orderNumber")(params)
      |@| parseNonEmpty("paymCallId")(params)
      |@| parsePaymentStatus("status")(params)
      |@| parseOptional("hakemusOid")(params)
      ) { (id, personOid, reference, orderNumber, paymCallId, status, hakemusOid) =>
      Payment(id, personOid, _:Date, reference, orderNumber, paymCallId, status, hakemusOid)
    }
  }

  def parsePaymentStatus(key: String)(params: Params) = params.get(key) match { case Some(a) => Try(PaymentStatus.withName(a).successNel).recover {
    case e => e.getMessage.failureNel
  }.get
  case _ => s"Parameter $key does not exist".failureNel
  }
}
