import Bacon from 'baconjs'
import _ from 'lodash'

import HttpUtil from '../util/HttpUtil.js'
import {enableSubmitAndHideBusy} from '../util/HtmlUtils.js'
import {ID_BIRTH_DATE, ID_PERSONAL_IDENTITY_CODE} from '../util/Constants.js'

export function submitUserDataToServer(state) {
  var userData = {
    firstName: state.firstName,
    lastName: state.lastName,
    gender: state.gender,
    nativeLanguage: state.nativeLanguage,
    nationality: state.nationality
  }

  const selectedId = state.idSelection
  if (selectedId === ID_PERSONAL_IDENTITY_CODE) {
    userData.personId = state.personId;
  } else if (selectedId === ID_BIRTH_DATE) {
    userData.birthDate = state.birthDate;
  } else {
    throw new Error("Invalid identification state to send, selected value: " + selectedId)
  }

  const promise = Bacon.fromPromise(HttpUtil.post(state.properties.userDataUrl, userData))
  promise.onError((error) => {
    const form = document.getElementById('userDataForm')
    enableSubmitAndHideBusy(form)
    if (error.status == 409) {
      form.querySelector("span.invalid").classList.remove("hide")
    } if (error.status == 412) {
      form.querySelector("span.alreadyused").classList.remove("hide")
    } else {
      form.querySelector("span.general").classList.remove("hide")
    }
  })
  return promise
}
