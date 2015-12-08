import React from 'react'
import _ from 'lodash'

import PaymentForm from './PaymentForm.jsx'

export default class PaymentGroup extends React.Component {
  constructor(props) {
    super()
    this.id = "paymentsGroup"
  }

  render() {
    const state = this.props.state
    const controller = this.props.controller
    const applicationObjects = _.isEmpty(state.applicationObjects) ? [] : state.applicationObjects
    const payments = _.isEmpty(state.payments) ? [] : state.payments
    const paymentsTitle = payments.length > 0 ? "Maksut" : "Hakijalla ei ole maksuja"
    const paymentsStatus = state.hasPaid ? "Maksettu" : "Kesken"
    const disabled = payments.length > 0 ? undefined : "disabled"
    return <div>
          <h3>{paymentsTitle}</h3>
              <div className="userDataFormRow">
                <label htmlFor={this.id}>Tila</label>
                <span>{paymentsStatus}</span>
              </div>
              <div className="userDataFormRow">
                <input type="submit" value="Näytä maksuloki" onClick={controller.pushTogglePaymentGroup} disabled={disabled}/>
              </div>
            <div id="paymentsGroup" className={state.showPaymentGroup?'':'hidden'} >
            {payments.map((payment,i) => {
              return <PaymentForm key={i} state={state} controller={controller} payment={payment}/>
              })}
            </div>
    </div>

  }

}