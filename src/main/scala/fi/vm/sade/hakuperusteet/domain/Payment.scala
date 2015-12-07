package fi.vm.sade.hakuperusteet.domain

import java.util.Date

import fi.vm.sade.hakuperusteet.domain.PaymentStatus.PaymentStatus

case class Payment(id: Option[Int], personOid: String, timestamp: Date, reference: String, orderNumber: String, paymCallId: String, status: PaymentStatus, hakemusOid: Option[String])

object PaymentStatus extends Enumeration {
  type PaymentStatus = Value
  val started, ok, cancel, error = Value
}
