import React from 'react'
import _ from 'lodash'

import EducationForm from './education/EducationForm.jsx'
import UserDataForm from './userdata/UserDataForm.jsx'
import PartialUserDataForm from './userdata/PartialUserDataForm.jsx'
import PaymentForm from './payment/PaymentForm.jsx'
import PartialUserPaymentForm from './payment/PartialUserPaymentForm.jsx'

export default class HakuperusteetAdminForm extends React.Component {
    constructor(props) {
        super()
        this.changes = props.controller.pushEducationFormChanges
    }

    render() {
        const state = this.props.state
        const controller = this.props.controller
        const isUserSelected = state.id ? true : false
        const applicationObjects = _.isEmpty(state.applicationObjects) ? [] : state.applicationObjects
        const payments = _.isEmpty(state.payments) ? [] : state.payments
        const paymentsTitle = payments.length > 0 ? "Maksut" : "Hakijalla ei ole maksuja"
        const paymentsStatus = state.hasPaid ? "Maksettu" : "Kesken"
        if(isUserSelected) {
            if(state.partialUser) {
                return <section className="main-content oppija">
                    <PartialUserDataForm state={state} controller={controller} />
                    <h3>{paymentsTitle}</h3>
                    {payments.map((payment,i) => {
                      return <PartialUserPaymentForm key={i} state={state} controller={controller} payment={payment}/>
                      })}
                </section>
            }
            return <section className="main-content oppija">
                <UserDataForm state={state} controller={controller} />
                <h3>{paymentsTitle}</h3>
                  <div className="userDataFormRow">
                    <label htmlFor={this.id}>Tila</label>
                    <span>{paymentsStatus}</span>
                  </div>

                {payments.map((payment,i) => {
                    return <PaymentForm key={i} state={state} controller={controller} payment={payment}/>
                })}
                <h3>Hakukohteet</h3>
                {applicationObjects.map((applicationObject,i) => {
                    return <EducationForm key={i} state={state} controller={controller} applicationObject={applicationObject}/>
                })}
            </section>
        } else {
            return <section/>;
        }
    }

}