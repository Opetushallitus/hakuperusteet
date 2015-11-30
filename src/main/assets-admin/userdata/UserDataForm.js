import Bacon from 'baconjs'

import HttpUtil from '../../assets/util/HttpUtil.js'
import {enableSubmitAndHideBusy} from '../../assets/util/HtmlUtils.js'
import {ID_BIRTH_DATE, ID_PERSONAL_IDENTITY_CODE} from '../../assets/util/Constants.js'

export function submitUserDataToServer(state) {
    const userData = {
        id: state.id,
        email: state.email,
        firstName: state.firstName,
        lastName: state.lastName,
        personOid: state.personOid,
        gender: state.gender,
        nativeLanguage: state.nativeLanguage,
        nationality: state.nationality,
        idpentityid: state.idpentityid,
        uiLang: state.uiLang
    }

    const selectedId = state.idSelection
    if (selectedId === ID_PERSONAL_IDENTITY_CODE) {
        userData.personId = state.personId;
    } else if (selectedId === ID_BIRTH_DATE) {
        userData.birthDate = state.birthDate;
    } else {
        throw new Error("Invalid identification state to send, selected value: " + selectedId)
    }

    const promise = Bacon.fromPromise(HttpUtil.post(state.userUpdateUrl, userData))
    promise.onError((error) => {
        const form = document.getElementById('userDataForm')
        enableSubmitAndHideBusy(form)
        if (error.status == 409) {
            form.querySelector("span.invalid").classList.remove("hide")
        } else {
            form.querySelector("span.general").classList.remove("hide")
        }
    })
    return promise
}
