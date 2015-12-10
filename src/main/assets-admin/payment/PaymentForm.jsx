import React from 'react'
import _ from 'lodash'

import {tarjontaForHakukohdeOid} from "../../assets/util/TarjontaUtil.js"

import {createSelectOptions} from '../../assets/util/HtmlUtils.js'
import HttpUtil from '../../assets/util/HttpUtil'
import AjaxLoader from '../util/AjaxLoader.jsx'
import {requiredField} from '../../assets/util/FieldValidator.js'
import {validatePayment} from '../util/PaymentValidator.js'
import {translation} from '../../assets-common/translations/translations.js'

export default class PaymentForm extends React.Component {
  constructor(props) {
    super()
    this.id = "payments"
  }

  render() {
    const controller = this.props.controller
    const payment = this.props.payment
    const formId = "payment_" + payment.id
    const statusId = "paymentStatus_" + payment.id
    console.log(payment)
    return <div className="paymentLogRow">
      <div className="userDataFormRow">
        <label htmlFor={this.id}>Aikaleima</label>
        <span>{payment.timestamp}</span>
      </div>
      <div className="userDataFormRow">
        <label htmlFor={this.id}>Viitenumero</label>
        <span>{payment.reference}</span>
      </div>
      {payment.hakemusOid ? <div className="userDataFormRow"><label htmlFor={statusId}>Hakemus OID</label><span>{payment.hakemusOid}</span></div> : null}
      <div className="userDataFormRow">
          <label htmlFor={statusId}>Maksun tila</label>
          <span>{payment.status}</span>
      </div>
        {payment.history.length > 0 ?
      <div className="userDataFormRow">
        <label htmlFor={statusId}>Maksun tilan muutokset</label>
        <span>{payment.history.map((h,i) => {return <span class="tooltip" data-tip={h.created}>{h.old_status}&nbsp;&rarr;&nbsp;</span>})}<u><strong>{payment.history[payment.history.length -1].new_status}</strong></u></span>
      </div> : null}
      </div>
  }

}