import React from 'react'
import _ from 'lodash'

import UserDataInput from '../../assets/userdata/UserDataInput.jsx'
import IdentificationInput from '../../assets/userdata/Identification.jsx'
import Gender from '../../assets/userdata/Gender.jsx'
import Nationality from '../../assets/userdata/Nationality.jsx'
import NativeLanguage from '../../assets/userdata/NativeLanguage.jsx'
import AjaxLoader from '../util/AjaxLoader.jsx'
import UserDataErrors from '../../assets/userdata/UserDataErrors.jsx'

import {validateUserDataForm, requiredField} from '../../assets/util/FieldValidator.js'
import {translation} from '../../assets-common/translations/translations.js'

export default class UserDataForm extends React.Component {
  render() {
    const state = this.props.state
    const controller = this.props.controller
    const disabled = (validateUserDataForm(state) && !requiredField(state, "noChanges")) ? undefined : "disabled"
    const languages = _.isUndefined(state.properties) ? [] : state.properties.languages
    const countries = _.isUndefined(state.properties) ? [] : state.properties.countries
    const errors = requiredField(state, "noChanges") ? <div className="userDataFormRow">
      <span className="error">Lomakkeella ei ole muuttuneita tietoja</span>
    </div> : <UserDataErrors state={state} controller={controller} />

    return <form id="userDataForm" onSubmit={controller.formSubmits}>
      <h2>{state.firstName}&nbsp;{state.lastName}</h2>
      <hr/>
      <div className="userDataFormRow">
        <label>{translation("title.email")}</label>
        <span>{state.email}</span>
      </div>
      <UserDataInput name="firstName" translation="title.first.name" required={true} state={state} controller={controller} />
      <UserDataInput name="lastName" translation="title.last.name" required={true} state={state} controller={controller} />
      <IdentificationInput state={state} controller={controller} />
      <Gender state={state} controller={controller} />
      <NativeLanguage state={state} languages={languages} controller={controller} />
      <Nationality state={state} countries={countries} controller={controller} />
      <div className="userDataFormRow">
        <input type="submit" name="submit" value={translation("userdataform.submit")} disabled={disabled} />
        <AjaxLoader hide={true} />
        <span className="serverError invalid hide">{translation("errors.server.invalid.userdata")}</span>
        <span className="serverError general hide">{translation("errors.server.unexpected")}</span>
      </div>
      {errors}
    </form>
  }
}
