package fi.vm.sade.hakuperusteet.domain

import java.util.Date

case class PaymentEvent(id: Option[Int], paymentId: Int, created: Date, timestamp: Option[Date], checkSucceeded: Boolean, paymentStatus: String)