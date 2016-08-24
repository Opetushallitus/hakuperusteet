package fi.vm.sade.hakuperusteet.domain

import java.util.Date

import fi.vm.sade.hakuperusteet.domain.Hakukausi.Hakukausi
import fi.vm.sade.hakuperusteet.domain.PaymentStatus.PaymentStatus

case class Payment(id: Option[Int], personOid: String, timestamp: Date, reference: String, orderNumber: String, paymCallId: String, status: PaymentStatus, kausi: Hakukausi, hakemusOid: Option[String], history: Option[Seq[PaymentEvent]] = None)

object PaymentStatus extends Enumeration {
  type PaymentStatus = Value
  val started, ok, cancel, error, unknown = Value
}


