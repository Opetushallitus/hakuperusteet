import React from 'react'

import {requiredField, invalidField} from '../util/FieldValidator.js'

import {translation} from '../../assets-common/translations/translations.js'

export default class UserDataErrors extends React.Component {
  render() {
    const state = this.props.state
    return <div className="userDataFormRow">
      { requiredField(state, "email") ? <span className="error">{translation("userdataform.errors.requiredEmail")}</span> : null}
      { invalidField(state, "email") ? <span className="error">{translation("userdataform.errors.invalidEmail")}</span> : null}
      { requiredField(state, "firstName") ? <span className="error">{translation("userdataform.errors.requiredFirstName")}</span> : null}
      { invalidField(state, "firstName") ? <span className="error">{translation("userdataform.errors.invalidFirstName")}</span> : null}
      { requiredField(state, "lastName") ? <span className="error">{translation("userdataform.errors.requiredLastName")}</span> : null}
      { invalidField(state, "lastName") ? <span className="error">{translation("userdataform.errors.invalidLastName")}</span> : null}
      { invalidField(state, "birthDate") ? <span className="error">{translation("userdataform.errors.invalidBirthDate")}</span> : null}
      { requiredField(state, "personId") ? <span className="error">{translation("userdataform.errors.requiredPersonId")}</span> : null}
      { requiredField(state, "gender") ? <span className="error">{translation("userdataform.errors.requiredGender")}</span> : null}
      { requiredField(state, "nativeLanguage") ? <span className="error">{translation("userdataform.errors.requiredNativeLanguage")}</span> : null}
      { requiredField(state, "nationality") ? <span className="error">{translation("userdataform.errors.requiredNationality")}</span> : null}
    </div>
  }
}
