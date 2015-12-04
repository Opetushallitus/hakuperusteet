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

    togglePayments() {
        var pg = document.getElementById("paymentsGroup");
        pg.style.display = (pg.style.display == 'none') ? 'block' : 'none';
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
                  <div className="paymentsShowLink">
                      <div className="userDataFormRow">
                        <input type="submit" value="Näytä maksuloki" onClick={this.togglePayments.bind(this)}/>
                      </div>
                  </div>
                <div id="paymentsGroup" style={{border: '2px solid #ccc', display: 'none'}}>
                {payments.map((payment,i) => {
                    return <PaymentForm key={i} state={state} controller={controller} payment={payment}/>
                })}
                </div>
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