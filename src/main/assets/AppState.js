import Bacon from 'baconjs'
import _ from 'lodash'

import HttpUtil from './util/HttpUtil.js'
import Dispatcher from './util/Dispatcher'
import {initGoogleAuthentication} from './session/GoogleAuthentication'
import {isLoginToken, initEmailAuthentication} from './session/EmailAuthentication'
import {initChangeListeners} from './util/ChangeListeners'
import {parseNewValidationErrors} from './util/FieldValidator.js'
import {submitUserDataToServer} from './userdata/UserDataForm.js'
import {submitEducationDataToServer} from './education/EducationForm.js'
import {resolveLang, setLang} from '../assets-common/translations/translations.js'

const dispatcher = new Dispatcher()
const events = {
  updateField: 'updateField',
  submitForm: 'submitForm',
  fieldValidation: 'fieldValidation',
  logOut: 'logOut',
  changeLang: 'changeLang'
}

export function changeListeners() {
  return initChangeListeners(dispatcher, events)
}

export function initAppState(props) {
  const {tarjontaUrl, propertiesUrl, sessionUrl, authenticationUrl} = props
  const initialState = {}

  const gapiLoading = Bacon.fromPoll(10, checkGapiStatus).filter(skipLoadingMessages)
  const serverUpdatesBus = new Bacon.Bus()
  const cssEffectsBus = new Bacon.Bus()
  const tarjontaLoadBus = new Bacon.Bus()
  const propertiesS = Bacon.fromPromise(HttpUtil.get(propertiesUrl))
  const hakukohdeS = tarjontaLoadBus.merge(Bacon.once(parseAoId())).toProperty()
  const hakemusS = tarjontaLoadBus.merge(Bacon.once(parseAppId())).toProperty()
  const tarjontaS = hakukohdeS.filter(isNotEmpty).flatMap(fetchFromTarjonta).toEventStream()

  const hashS = propertiesS.flatMap(locationHash).filter(isNotEmpty)
  const sessionS = propertiesS.flatMap(sessionFromServer(sessionUrl))
  const emailUserS = hashS.filter(isLoginToken).flatMap(initEmailAuthentication)
  const googleUserS = gapiLoading.concat(propertiesS.toProperty()).flatMap(initGoogleAuthentication)

  const sessionOkS = sessionS.filter(isNotEmpty)
  const noSessionAuthenticateWithGoogle = sessionS.combine(googleUserS, toArray).filter(sessionFromServerEmpty).map(user).filter(isNotEmpty).flatMap(authenticate(authenticationUrl))
  const noSessionAuthenticateWithEmail = sessionS.combine(emailUserS, toArray).map(user).filter(isNotEmpty).flatMap(authenticate(authenticationUrl))
  const sessionOkGoogleS = noSessionAuthenticateWithGoogle.filter(isNotEmpty)

  const sessionDataS = sessionOkS.merge(sessionOkGoogleS).merge(noSessionAuthenticateWithEmail).doAction(sessionInit)
  const otherApplicationObjects = sessionDataS.flatMap(applicationObjects).map(".hakukohdeOid").filter(notCurrentHakukohde).toEventStream()
  tarjontaLoadBus.plug(otherApplicationObjects)

  cssEffectsBus.plug(hashS.filter(isCssEffect).flatMap(toCssEffect))

  const updateFieldS = dispatcher.stream(events.updateField).merge(serverUpdatesBus)
  const fieldValidationS = dispatcher.stream(events.fieldValidation)
  const logOutS = dispatcher.stream(events.logOut)
  const changeLangS = dispatcher.stream(events.changeLang)

  const stateP = Bacon.update(initialState,
    [propertiesS, hakukohdeS, hakemusS], onStateInit,
    [tarjontaS], onTarjontaValue,
    [cssEffectsBus], onCssEffectValue,
    [sessionDataS], onSessionDataFromServer,
    [updateFieldS], onUpdateField,
    [logOutS], onLogOut,
    [fieldValidationS], onFieldValidation,
    [changeLangS], onChangeLang,
    [hashS], onChangeHash)

  const formSubmittedS = stateP.sampledBy(dispatcher.stream(events.submitForm), (state, form) => ({state, form}))

  const userDataFormSubmittedP = formSubmittedS
    .filter(({form}) => form === 'userDataForm')
    .flatMapLatest(({state}) => submitUserDataToServer(state))
  serverUpdatesBus.plug(userDataFormSubmittedP)

  const educationFromSubmittedP = formSubmittedS
    .filter(({form}) => form === 'educationForm')
    .flatMapLatest(({state}) => submitEducationDataToServer(state))
  serverUpdatesBus.plug(educationFromSubmittedP)

  return stateP

  function onStateInit(state, properties, hakukohdeOid, hakemusOid) {
    return {...state, properties, hakukohdeOid, hakemusOid}
  }

  function onTarjontaValue(state, tarjonta) {
    const currentTarjonta = state.tarjonta || []
    const newTarjonta = {...currentTarjonta, [tarjonta.hakukohdeOid]: tarjonta}
    return {...state, ['tarjonta']: newTarjonta}
  }

  function onCssEffectValue(state, effect) {
    if (effect !== "") {
      Bacon.once("").take(1).delay(1500000).onValue((x) => cssEffectsBus.push(x))
    }
    return {...state, effect}
  }

  function onUpdateField(state, {field, value}) {
    return {...state, [field]: value}
  }

  function onLogOut() {
    window.location = '/hakuperusteet/#logoutPage'
    window.location.reload()
  }

  function onChangeLang(state, {field, lang}) {
    setLang(lang)
    return {...state, ['lang']: lang}
  }

  function onChangeHash(state, newHash) {
    return {...state, isLogoutPage: newHash === '#logoutPage'}
  }

  function onSessionDataFromServer(state, sessionData) {
    return {...state, sessionData}
  }

  function onFieldValidation(state, {field, value}) {
    const newValidationErrors = parseNewValidationErrors(state, field, value)
    return {...state, ['validationErrors']: newValidationErrors}
  }

  function locationHash() {
    var currentHash = location.hash
    location.hash = ""
    return Bacon.once(currentHash)
  }
  function isNotEmpty(x) { return !_.isEmpty(x) }
  function toArray(l, r) { return [l, r] }
  function sessionFromServerEmpty([l, r]) { return _.isEmpty(l) }
  function user([l, r]) { return r }
  function isCssEffect(x) { return _.startsWith(x, "#/effect/") }
  function toCssEffect(x) { return x.replace("#/effect/", "") }
  function checkGapiStatus() {
    if (typeof gapi == "undefined") return new Bacon.Next("loading")
    else return new Bacon.End()
  }
  function skipLoadingMessages(x) { return x != "loading" }
  function parseAppId() {
    const hakemusOid = /\/hakuperusteet\/app\/([0-9\.]*)\/?$/
    return hakemusOid.test(location.pathname) ? hakemusOid.exec(location.pathname).pop() : ""
  }
  function parseAoId() {
    const aoid = /\/hakuperusteet\/ao\/([0-9\.]*)\/?$/
    return aoid.test(location.pathname) ? aoid.exec(location.pathname).pop() : ""
  }
  function applicationObjects(s) { return Bacon.fromArray(s.applicationObject) }
  function notCurrentHakukohde(x) { return x != parseAoId() }
  function fetchFromTarjonta(hakukohde) {
    return Bacon.fromPromise(HttpUtil.get(tarjontaUrl + "/" + hakukohde)).doError(handleTarjontaError).skipErrors()
  }
}

function authenticate(authenticationUrl) {
  return (user) => Bacon.fromPromise(HttpUtil.post(authenticationUrl, user)).doAction(removeAuthenticationError).doError(handleAuthenticationError).skipErrors()
}

function sessionFromServer(sessionUrl) {
  return (user) => Bacon.fromPromise(HttpUtil.get(sessionUrl, user)).doError(sessionInit).mapError("")
}

function removeAuthenticationError(_) {
  dispatcher.push(events.updateField, {field: 'authenticationError', value: false})
}

function handleAuthenticationError(e) {
  if (e.status == 401) {
    dispatcher.push(events.updateField, {field: 'authenticationError', value: true})
  }
}

function handleTarjontaError(_) {
  dispatcher.push(events.updateField, {field: 'serverError', value: true})
}

function sessionInit() {
  document.domain = location.hostname
  window.SESSION_INITED_FOR_TESTING = true
  dispatcher.push(events.updateField, {field: 'sessionInit', value: true})
}
