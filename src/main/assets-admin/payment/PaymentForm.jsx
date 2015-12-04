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
    this.changes = props.controller.pushPaymentFormChanges
  }

  componentDidMount() {
    this.changes(this.props.payment)
  }

  render() {
    const controller = this.props.controller
    const payment = this.props.payment
    const statusOptions = [{ id: "started", name: "Started"}, { id: "ok", name: "Ok"}, { id: "cancel", name: "Cancel"}, { id: "error", name: "Error"}]
    const formId = "payment_" + payment.id
    const statusId = "paymentStatus_" + payment.id

    const disabled = (validatePayment(payment) && !requiredField(payment, "noChanges")) ? undefined : "disabled"
    const errors = requiredField(payment, "noChanges") ? <div className="userDataFormRow">
      <span className="error">Lomakkeella ei ole muuttuneita tietoja</span>
    </div> : <div className="userDataFormRow">
                      { requiredField(payment, "status") ? <span className="error">Maksun tila on pakollinen tieto</span> : null}
    </div>

    return <div>
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
      {errors}
      </div>
  }

}