import React from 'react'

import {translation} from '../../assets-common/translations/translations.js'

export default class UserDataInput extends React.Component {
  constructor(props) {
    super()
    this.translation = props.translation
    this.name = props.name
    this.changes = props.controller.valueChanges
    this.required = props.required
    this.disabled = props.disabled
  }

  componentDidMount() {
    if (_.isEmpty(this.props.state[this.name])) this.changes({ target: { id: this.name, value: "" }})
  }

  render() {
    return <div className="userDataFormRow">
        <label htmlFor={this.name}>{translation(this.translation)} {this.required?"*":""}</label>
        <input disabled={this.disabled} type="text" id={this.name} name={this.name} onChange={this.changes} onBlur={this.changes} maxLength="255" value={this.props.state[this.name]}/>
      </div>
  }
}
