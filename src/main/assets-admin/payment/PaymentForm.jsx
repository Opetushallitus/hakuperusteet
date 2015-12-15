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
    const changes = this.props.controller.pushPaymentFormChanges
    const controller = this.props.controller
    const payment = this.props.payment
    const formId = "payment_" + payment.id
    const statusId = "paymentStatus_" + payment.id
    const isOph = this.props.state.sessionData.oph

    const statusOptions = [{ id: "started", name: "Started"}, { id: "ok", name: "Ok"}, { id: "cancel", name: "Cancel"}, { id: "error", name: "Error"}]
    const result = createSelectOptions(statusOptions)
    const disabled = (validatePayment(payment) && !requiredField(payment, "noChanges")) ? undefined : "disabled"
    const errors = requiredField(payment, "noChanges") ? <div className="userDataFormRow">
      <span className="error">Lomakkeella ei ole muuttuneita tietoja</span>
    </div> : <div className="userDataFormRow">
      { requiredField(payment, "status") ? <span className="error">Maksun tila on pakollinen tieto</span> : null}
    </div>

    return <form className="paymentLogRow" id={formId} onSubmit={controller.formSubmits}>
      <div className="userDataFormRow">
        <label htmlFor={this.id}>Aikaleima</label>
        <span>{payment.timestamp}</span>
      </div>
      <div className="userDataFormRow">
        <label htmlFor={this.id}>Tilausnumero</label>
        <span>{payment.orderNumber}</span>
      </div>
      {payment.hakemusOid ? <div className="userDataFormRow"><label htmlFor={statusId}>Hakemus OID</label><span>{payment.hakemusOid}</span></div> : null}
      {isOph ? <div className="userDataFormRow">
        <label>Maksun tila</label>
        <select id={statusId} name="status" onChange={changes.bind(this, payment)}  onBlur={changes.bind(this, payment)} value={payment.status}>
          {result}
        </select>
      </div> : <div className="userDataFormRow">
        <label htmlFor={statusId}>Maksun tila</label>
        <span>{payment.status}</span>
      </div>
        }
        {payment.history.length > 0 ?
      <div className="userDataFormRow">
        <label htmlFor={statusId}>Maksun tilan muutokset</label>
        <span>{payment.history.map((h,i) => {return <span class="tooltip" data-tip={h.created}>{h.old_status}&nbsp;&rarr;&nbsp;</span>})}<u><strong>{payment.history[payment.history.length -1].new_status}</strong></u></span>
      </div> : null}
      {isOph ?
        <div>
      <div className="userDataFormRow">
        <input type="submit" name="submit" value="Submit" disabled={disabled}/>
        <AjaxLoader hide={true}/>
        <span className="serverError general hide">{translation("errors.server.unexpected")}</span>
      </div> {errors} </div>
        : null
        }
      </form>
  }

}