import {disableSubmitAndShowBusy} from './HtmlUtils.js'

export function initChangeListeners(dispatcher, events) {
  function valueChanges(e) {
    const field = e.target.id
    const value = e.target.value
    pushChangeAndValidation(field, value)
  }

  function checkedChanges(e) {
    const field = e.target.id
    const value = e.target.checked
    pushChangeAndValidation(field, value)
  }

  function radioChanges(e) {
    const field = e.target.name
    const value = e.target.value
    pushChangeAndValidation(field, value)
  }

  function formSubmits(e) {
    e.preventDefault()
    const form = document.getElementById(e.target.id)
    disableSubmitAndShowBusy(form)
    dispatchFormSubmitEvent(e.target.id)
  }

  function dispatchFormSubmitEvent(formId) {
    dispatcher.push(events.submitForm, formId)
  }

  function pushChangeAndValidation(field, value) {
    dispatcher.push(events.updateField, {field: field, value: value})
    dispatcher.push(events.fieldValidation, {field: field, value: value})
  }

  function logOut() {
    dispatcher.push(events.logOut, {})
  }

  function changeLang(lang) {
    return () => {
      dispatcher.push(events.changeLang, {lang: lang})
    }
  }

  return {
    pushChangeAndValidation: pushChangeAndValidation,
    valueChanges: valueChanges,
    checkedChanges: checkedChanges,
    radioChanges: radioChanges,
    formSubmits: formSubmits,
    dispatchFormSubmitEvent: dispatchFormSubmitEvent,
    logOut: logOut,
    changeLang: changeLang
  }
}

