import React from 'react'
import _ from 'lodash'

import PaymentForm from './PaymentForm.jsx'

export default class PaymentFormForKausi extends React.Component {
    constructor(props) {
        super()
        this.id = "paymentsForKausi"
    }

    render() {
        const state = this.props.state
        const controller = this.props.controller
        const payments = this.props.payments
        const hasPaid = _.any(payments, function(p) {return p.status == 'ok'})
        const paymentsStatus = hasPaid ? "Maksettu" : "Kesken"
        const disabled = payments.length > 0 ? undefined : "disabled"
        const showPaymentGroup = "s2016" == this.props.kausi ? state.showPaymentGroupS2016 : state.showPaymentGroupK2017
        return payments.length > 0 ? <div>
            <h3>Maksut {this.props.kausi}</h3>
            <div className="userDataFormRow">
                <label htmlFor={this.id + this.props.kausi}>Tila</label>
                <span>{paymentsStatus}</span>
            </div>
            <div className="userDataFormRow">
                <label>Viitenumero</label>
                <span>{payments[0].reference}</span>
            </div>
            <div className="userDataFormRow">
                <input id={this.props.kausi + "TogglePaymentGroup"} type="submit" value="Näytä maksuloki" onClick={controller.pushTogglePaymentGroup} disabled={disabled}/>
            </div>
            <div id={"paymentsGroup" + this.props.kausi} className={showPaymentGroup ?'':'hidden'} >
                {payments.map((payment,i) => {
                    return <PaymentForm key={i} state={state} controller={controller} payment={payment}/>
                })}
            </div>
        </div>
            :
        <div>
            <h3>Hakijalla ei ole maksuja hakumaksukaudelle {this.props.kausi}</h3>
        </div>
    }
}