import React from 'react'

import AjaxLoader from '../util/AjaxLoader.jsx'

export default class EducationWithoutPaymentForm extends React.Component {

  constructor(props) {
    super()
    props.controller.dispatchFormSubmitEvent("educationWithoutPaymentForm")
  }

  render() {
    return <form id="educationWithoutPaymentForm">
      <div className="userDataFormRow">
        <AjaxLoader/>
      </div>
    </form>
  }
}
