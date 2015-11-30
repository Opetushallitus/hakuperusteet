import React from 'react'
import _ from 'lodash'

import {ID_BIRTH_DATE, ID_PERSONAL_IDENTITY_CODE} from '../util/Constants'
import {translation, resolveLang} from '../../assets-common/translations/translations.js'

export default class IdentificationInput extends React.Component {
  constructor(props) {
    super()
    this.radioChanges = props.controller.radioChanges
    this.valueChanges = props.controller.valueChanges
  }

  componentDidMount() {
    // Convenience pre-select PIC if finnish or swedish UI
    const expectUserToHavePersonalIdCode = ["fi", "sv"].indexOf(resolveLang()) !== -1
    if (_.isEmpty(this.props.state["idSelection"]) && expectUserToHavePersonalIdCode) {
      this.radioChanges({ target: { name: "idSelection", value: ID_PERSONAL_IDENTITY_CODE }})
    }
  }

  render() {
    const {state} = this.props
    const controller = this.props.controller
    const radioSelect = state["idSelection"];

    return <div className="userDataFormRow">
      <label>{translation("title.identification") + " *"}</label>
      <ul className="personalIdCheck">
        <li>
          <input type="radio"
                 id="personal-id-yes"
                 name="idSelection"
                 value={ID_PERSONAL_IDENTITY_CODE}
                 onChange={controller.radioChanges}
                 checked={radioSelect === ID_PERSONAL_IDENTITY_CODE}/>
          <label htmlFor="personal-id-yes">{translation("userdataform.personalIdentityCodeYes")}</label>
          <input type="text"
                 id="personId"
                 name="personId"
                 onChange={this.valueChanges}
                 onBlur={this.valueChanges}
                 maxLength="11"
                 value={state["personId"]}
                 disabled={radioSelect !== ID_PERSONAL_IDENTITY_CODE}/>
          <span className="fieldFormatInfo">{translation("userdataform.personalIdentityCodeInfo")}</span>
        </li>
        <li>
          <input type="radio"
                 id="personal-id-no"
                 name="idSelection"
                 value={ID_BIRTH_DATE}
                 onChange={controller.radioChanges}
                 checked={radioSelect === ID_BIRTH_DATE}/>
          <label htmlFor="personal-id-no">{translation("userdataform.personalIdentityCodeNo")}</label>
          <input type="text"
                 id="birthDate"
                 name="birthDate"
                 onChange={this.valueChanges}
                 onBlur={this.valueChanges}
                 maxLength="8"
                 value={state["birthDate"]}
                 disabled={radioSelect !== ID_BIRTH_DATE}/>
          <span className="fieldFormatInfo">{translation("userdataform.birthdayInfo")}</span>
        </li>
      </ul>
    </div>
  }

}
