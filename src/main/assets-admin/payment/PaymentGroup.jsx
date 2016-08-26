import React from 'react'
import _ from 'lodash'

import PaymentFormForKausi from './PaymentFormForKausi.jsx'

export default class PaymentGroup extends React.Component {
  constructor(props) {
    super()
    this.id = "paymentsGroup"
  }

  render() {
    const state = this.props.state
    const controller = this.props.controller
    const payments = _.isEmpty(state.payments) ? [] : state.payments
    const s2016 = _.filter(payments, function(p) {return p.kausi == 's2016'})
    const k2017 = _.filter(payments, function(p) {return p.kausi == 'k2017'})

    return payments.length == 0 ?
        <div>
            <h3>Hakijalla ei ole maksuja</h3>
        </div> :
        <div>
      <PaymentFormForKausi key="s2016Payments" state={state} controller={controller} payments={s2016} kausi="s2016"/>
      <PaymentFormForKausi key="k2017Payments" state={state} controller={controller} payments={k2017} kausi="k2017"/>
      </div>
  }
}