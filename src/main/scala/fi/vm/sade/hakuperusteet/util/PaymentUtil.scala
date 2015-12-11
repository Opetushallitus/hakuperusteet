package fi.vm.sade.hakuperusteet.util

import fi.vm.sade.hakuperusteet.domain.PaymentStatus
import fi.vm.sade.hakuperusteet.domain.PaymentStatus.PaymentStatus
import fi.vm.sade.hakuperusteet.domain.{PaymentStatus, Payment}

object PaymentUtil {

  def getValidPayment(payments: Seq[Payment]): Option[Payment] = payments.find(_.status == PaymentStatus.ok)

  private val STATUS_ORDERING = List(PaymentStatus.ok, PaymentStatus.cancel, PaymentStatus.error, PaymentStatus.started)

  def sortPaymentsByStatus(payments: Seq[Payment]) = payments.sortBy(p => STATUS_ORDERING.indexOf(p.status))

  def hasPaid(payments: Seq[Payment]) = payments.exists(_.status.equals(PaymentStatus.ok))

  def hasPaidWithTheseStatuses(paymentStatuses: Seq[PaymentStatus]) = paymentStatuses.exists(_.equals(PaymentStatus.ok))

}
