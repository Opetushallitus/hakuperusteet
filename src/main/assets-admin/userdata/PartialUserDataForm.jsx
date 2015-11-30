import React from 'react'
import _ from 'lodash'

import UserDataInput from '../../assets/userdata/UserDataInput.jsx'
import Gender from '../../assets/userdata/Gender.jsx'
import Nationality from '../../assets/userdata/Nationality.jsx'
import NativeLanguage from '../../assets/userdata/NativeLanguage.jsx'
import AjaxLoader from '../util/AjaxLoader.jsx'
import UserDataErrors from '../../assets/userdata/UserDataErrors.jsx'

import {validateUserDataForm, requiredField} from '../../assets/util/FieldValidator.js'
import {translation} from '../../assets-common/translations/translations.js'

export default class PartialUserDataForm extends React.Component {
  render() {
    const state = this.props.state
    const controller = this.props.controller
    return <form id="userDataForm" onSubmit={controller.formSubmits}>
      <h2>{state.email}</h2>
      <hr/>
      <div className="userDataFormRow">
        <label>{translation("title.email")}</label>
        <span>{state.email}</span>
        <p>Sisälomakkeella luotuja hakijoita ei voi muokata tästä käyttöliittymästä.</p>
      </div>
    </form>
  }
}
