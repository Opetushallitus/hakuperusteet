package fi.vm.sade.hakuperusteet.validation

import java.util.Date

import fi.vm.sade.hakuperusteet.domain.{Hakukausi, Payment, PaymentStatus}
import fi.vm.sade.hakuperusteet.util.ValidationUtil

import scala.util.Try
import scalaz._
import scalaz.syntax.validation._

case class PaymentValidator() extends ValidationUtil {

  def parsePaymentWithoutTimestamp(params: Params) = {
    Apply[ValidationResult].apply8(
      parseOptionalInt("id")(params),
      parseNonEmpty("personOid")(params),
      parseNonEmpty("reference")(params),
      parseNonEmpty("orderNumber")(params),
      parseNonEmpty("paymCallId")(params),
      parsePaymentStatus("status")(params),
      parseHakukausi("kausi")(params),
      parseOptional("hakemusOid")(params)
    ) { (id, personOid, reference, orderNumber, paymCallId, status, kausi, hakemusOid) =>
      Payment(id, personOid, _: Date, reference, orderNumber, paymCallId, status, kausi, hakemusOid)
    }
  }

  def parsePaymentStatus(key: String)(params: Params) = params.get(key) match { case Some(a) => Try(PaymentStatus.withName(a).successNel[String]).recover {
    case e => e.getMessage.failureNel
  }.get
  case _ => s"Parameter $key does not exist".failureNel
  }

  def parseHakukausi(key: String)(params: Params) = params.get(key) match { case Some(a) => Try(Hakukausi.withName(a).successNel[String]).recover {
    case e => e.getMessage.failureNel
  }.get
  case _ => s"Parameter $key does not exist".failureNel
  }
}
