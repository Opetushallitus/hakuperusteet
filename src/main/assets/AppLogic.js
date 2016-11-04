import {tarjontaForHakukohdeOid} from "./util/TarjontaUtil.js"

export function sessionInit(state) {
  return !_.isUndefined(state.sessionInit) && state.sessionInit
}

export function fatalError(state) {
  return serverError(state) || !hakuperusteetInUseWithSelectedHakukohdeOid(state) || !hakuForSelectedHakukohdeOidIsOpen(state) || !hakuForSelectedHakukohdeOidIsJulkaistu(state)
}

export function serverError(state) {
  return !_.isUndefined(state.serverError) && state.serverError
}

export function maksumuuriInUseWithSelectedHakukohdeOid(state) {
  return _.isEmpty(state.hakukohdeOid) || tarjontaForHakukohdeOid(state, state.hakukohdeOid).maksumuuriKaytossa
}

export function hakuperusteetInUseWithSelectedHakukohdeOid(state) {
  return maksumuuriInUseWithSelectedHakukohdeOid(state) || tarjontaForHakukohdeOid(state, state.hakukohdeOid).tunnistusKaytossa
}

export function hakuForSelectedHakukohdeOidIsOpen(state) {
  if (_.isEmpty(state.hakukohdeOid)) {
    return true
  } else {
    const t = tarjontaForHakukohdeOid(state, state.hakukohdeOid)
    const startDate = new Date(t.startDate)
    const endDate = new Date(t.endDate)
    const now = new Date()
    return (startDate < now) && (now < endDate)
  }
}

export function hakuForSelectedHakukohdeOidIsJulkaistu(state) {
  if (_.isEmpty(state.hakukohdeOid)) {
    return true
  } else {
    return tarjontaForHakukohdeOid(state, state.hakukohdeOid).julkaistu
  }
}

export function showLoginInfo(state) {
  return sessionInit(state) && (_.isUndefined(state.sessionData) || _.isUndefined(state.sessionData.session) || _.isUndefined(state.sessionData.session.email))
}

export function hasGoogleSession(state) {
  return !_.isUndefined(state.sessionData) && !_.isUndefined(state.sessionData.session) && !_.isUndefined(state.sessionData.session.email) && state.sessionData.session.idpentityid == "google"
}

export function hasEmailSession(state) {
  return !_.isUndefined(state.sessionData) && !_.isUndefined(state.sessionData.session) && !_.isUndefined(state.sessionData.session.email) && state.sessionData.session.idpentityid == "oppijaToken"
}

export function hasAuthenticationError(state) {
  return !_.isUndefined(state.authenticationError) && state.authenticationError == true
}

export function showUserDataForm(state) {
  return !fatalError(state) && !_.isUndefined(state.sessionData) && !_.isUndefined(state.sessionData.session) && (_.isUndefined(state.sessionData.user) || isPartialUser(state))
}

export function showEducationForm(state) {
  return !fatalError(state) && maksumuuriInUseWithSelectedHakukohdeOid(state) && !isPartialUser(state) && hasUserData(state) && hasSelectedHakukohde(state) && !hasEducationForSelectedHakukohdeOid(state)
}

export function showEducationWithoutPaymentForm(state) {
  return !fatalError(state) && !maksumuuriInUseWithSelectedHakukohdeOid(state) && !isPartialUser(state) && hasUserData(state) && hasSelectedHakukohde(state) && !hasEducationForSelectedHakukohdeOid(state)
}

function hasNoValidPaymentForHakemus(state) {
  return _.all(paymentsForHakumaksukausi(state), function(p) { return p.status != "ok"})
}
export function showVetumaStartForHakemus(state) {
  return !fatalError(state) && hasUserData(state) && hasNoValidPaymentForHakemus(state)
}
export function isPartialUser(state) {
  return hasUserData(state) && state.sessionData.user.partialUser
}
export function showVetumaStart(state) {
  return !fatalError(state) && hasUserData(state) && (
      (!hasSelectedHakukohde(state) && !paymentsOkWhenNoHakukohdeSelected(state)) ||
      (hasEducationForSelectedHakukohdeOid(state) && paymentRequiredWithCurrentHakukohdeOid(state) && !hasValidPaymentForHakumaksukausi(state)))
}

function paymentsForHakumaksukausi(state) {
  return _.filter(state.sessionData.payment, function(p) { return !_.isUndefined(state.hakumaksukausi) && p.kausi == state.hakumaksukausi.hakumaksukausi })
}

export function isHakuAppView(state) {
  return !_.isEmpty(state.hakemusOid)
}

export function showHakuList(state) {
  return !fatalError(state) && hasUserData(state) && (
      (!hasSelectedHakukohde(state) && paymentsOkWhenNoHakukohdeSelected(state)) ||
      (hasEducationForSelectedHakukohdeOid(state) && (hasValidPaymentForHakumaksukausi(state) || !paymentRequiredWithCurrentHakukohdeOid(state))))
}

function paymentsOkWhenNoHakukohdeSelected(state) {
  return _.all(getUniqueHakukaudet(state), function(hk) {return hakumaksukausiNotRequirePayment(state, hk) })
}

function getUniqueHakukaudet(state) {
  return _.uniq(_.map(state.sessionData.applicationObject, function(ao){return ao.hakumaksukausi}))
}

function hakumaksukausiNotRequirePayment(state, hakumaksukausi) {
  return hasValidPaymentForGivenHakumaksukausi(state, hakumaksukausi) ||
      !_.some(getApplicationObjectsForHakumaksukausi(state, hakumaksukausi), function(ao) {return paymentRequiredWithCurrentHakukohdeOid(state, ao)})
}

export function getHakumaksukausiThatRequiresPaymentWhenNoHakukohdeSelected(state) {
  return _.find(getUniqueHakukaudet(state), function(hk) {return !hakumaksukausiNotRequirePayment(state, hk)})
}

function getApplicationObjectsForHakumaksukausi(state, hakumaksukausi) {
  return state.sessionData ? _.filter(state.sessionData.applicationObject, function(ao) {return ao.hakumaksukausi == hakumaksukausi}) : []
}

function hasValidPaymentForGivenHakumaksukausi(state, hakumaksukausi) {
  return state.sessionData && _.some(state.sessionData.payment, function(p) {return p.kausi == hakumaksukausi && p.status == "ok"})
}

export function hasValidPayment(state) {
  return state.sessionData && _.some(state.sessionData.payment, function(p) { return p.status == "ok"})
}

export function hasValidPaymentForHakumaksukausi(state) {
  return state.sessionData && _.some(paymentsForHakumaksukausi(state), function(p) { return p.status == "ok"})
}

export function showVetumaResultOk(state) {
  return !_.isUndefined(state.effect) && state.effect == "VetumaResultOk"
}

export function showVetumaResultCancel(state) {
  return !_.isUndefined(state.effect) && state.effect == "VetumaResultCancel"
}
export function showVetumaResultError(state) {
  return !_.isUndefined(state.effect) && state.effect == "VetumaResultError"
}

function hasUserData(state) {
  return !_.isUndefined(state.sessionData) && !_.isUndefined(state.sessionData.user)
}

function hasSelectedHakukohde(state) {
  return !_.isEmpty(state.hakukohdeOid)
}

function hasEducationForSelectedHakukohdeOid(state) {
  return !_.isEmpty(state.sessionData.applicationObject) && _.some(state.sessionData.applicationObject, (e) => { return e.hakukohdeOid == state.hakukohdeOid })
}

export function paymentRequiredWithCurrentHakukohdeOid(state, dataForAo) {
  const educationForCurrentHakukohdeOid = dataForAo || _.findWhere(state.sessionData.applicationObject, {hakukohdeOid: state.hakukohdeOid})
  if (!maksumuuriInUseWithSelectedHakukohdeOid(state) || !educationForCurrentHakukohdeOid.educationCountry) {
    return false
  } else {
    const eeaCountries = (state.properties && state.properties.eeaCountries) ? state.properties.eeaCountries : []
    const isEeaCountry = _.contains(eeaCountries, educationForCurrentHakukohdeOid.educationCountry)
    const isDiscretionaryEducationLevel = educationForCurrentHakukohdeOid.educationLevel === '106'
    return !(isEeaCountry || isDiscretionaryEducationLevel)
  }
}

function paymentRequired(state) {
  return state.sessionData.applicationObject.some((ao) => paymentRequiredWithCurrentHakukohdeOid(state, ao))
}