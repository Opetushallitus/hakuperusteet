import React from 'react'
import _ from 'lodash'

import UserDataInput from './UserDataInput.jsx'
import IdentificationInput from './Identification.jsx'
import Gender from './Gender.jsx'
import Nationality from './Nationality.jsx'
import NativeLanguage from './NativeLanguage.jsx'
import AjaxLoader from '../util/AjaxLoader.jsx'
import UserDataErrors from './UserDataErrors.jsx'

import {validateUserDataForm} from './../util/FieldValidator.js'
import {translation} from '../../assets-common/translations/translations.js'

export default class UserDataForm extends React.Component {
  render() {
    const state = this.props.state
    const controller = this.props.controller
    const disabled = (validateUserDataForm(state)) ? "" : "disabled"
    const languages = _.isUndefined(state.properties) ? [] : state.properties.languages
    const countries = _.isUndefined(state.properties) ? [] : state.properties.countries
    return <form id="userDataForm" onSubmit={controller.formSubmits}>
        <p>{translation("userdataform.info")}</p>
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
          <span className="serverError alreadyused hide">{translation("errors.server.ssnAlreadyExistsWithAnotherEmail")}</span>
        </div>
        <UserDataErrors state={state} controller={controller} />
      </form>
  }
}
