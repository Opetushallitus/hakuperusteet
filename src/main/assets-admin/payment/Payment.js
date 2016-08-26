import Bacon from 'baconjs'

import HttpUtil from '../../assets/util/HttpUtil.js'
import {enableSubmitAndHideBusy} from '../../assets/util/HtmlUtils.js'
import {tarjontaForHakukohdeOid} from "../../assets/util/TarjontaUtil.js"

export function submitPaymentDataToServer(state, payment, form) {
  const paymentData = {
    id: payment.id,
    orderNumber: payment.orderNumber,
    paymCallId: payment.paymCallId,
    personOid: payment.personOid,
    reference: payment.reference,
    status: payment.status,
    timestamp: payment.timestamp,
    kausi: payment.kausi
  }
  const promise = Bacon.fromPromise(HttpUtil.post(state.paymentUpdateUrl, paymentData))
  promise.onError((error) => {
    enableSubmitAndHideBusy(form)
    form.querySelector("span.general").classList.remove("hide")
  })
  return promise
}