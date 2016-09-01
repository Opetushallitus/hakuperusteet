import Bacon from 'baconjs'
import _ from 'lodash'

import {paymentRequiredWithCurrentHakukohdeOid} from '../assets/AppLogic.js'
import {submitUserDataToServer} from './userdata/UserDataForm.js'
import {submitEducationDataToServer} from './education/EducationForm.js'
import {submitPaymentDataToServer} from './payment/Payment.js'
import HttpUtil from '../assets/util/HttpUtil.js'
import Dispatcher from '../assets/util/Dispatcher'
import {initChangeListeners} from '../assets/util/ChangeListeners'
import {initAdminChangeListeners} from './util/ChangeListeners'
import {fieldValidationResults} from '../assets/util/FieldValidator.js'
import {applicationObjectWithValidationErrors} from './util/ApplicationObjectValidator.js'
import {paymentWithValidationErrors} from './util/PaymentValidator.js'
import {hideBusy} from '../assets/util/HtmlUtils.js'
import {ID_BIRTH_DATE, ID_PERSONAL_IDENTITY_CODE} from '../assets/util/Constants'
import {STUDENT_SEARCH_NAME_MIN_LENGTH} from './util/Config'

const dispatcher = new Dispatcher()
const events = {
    updatePaymentForm: 'updatePaymentForm',
    togglePaymentGroup: 'togglePaymentGroup',
    updateEducationForm: 'updateEducationForm',
    route: 'route',
    search: 'search',
    updateField: 'updateField',
    submitForm: 'submitForm',
    fieldValidation: 'fieldValidation',
    logOut: 'logOut'
}

export function changeListeners() {
    const changeListeners = initChangeListeners(dispatcher, events)
    const adminChangeListeners = initAdminChangeListeners(dispatcher, events)
    return {...changeListeners, ...adminChangeListeners}
}

export function initAppState(props) {
    document.domain = location.hostname
    const {hakuperusteetadmin:{tarjontaUrl, propertiesUrl, userUpdateUrl, applicationObjectUpdateUrl, paymentUpdateUrl}} = props
    const initialState = {
        ['userUpdateUrl']:userUpdateUrl,
        ['applicationObjectUpdateUrl']:applicationObjectUpdateUrl,
        ['paymentUpdateUrl']:paymentUpdateUrl
    }
    const propertiesS = Bacon.fromPromise(HttpUtil.get(propertiesUrl))
    const serverUpdatesBus = new Bacon.Bus()
    const searchS = dispatcher.stream(events.search).debounce(250).skipDuplicates(_.isEqual)
    const fetchUsersFromServerS =
      searchS.flatMapLatest(search => {
          // Show no results if query is too short (avoid flooding UI with too many results)
          if (search.length < STUDENT_SEARCH_NAME_MIN_LENGTH)
            return Bacon.once([])
          return Bacon.fromPromise(HttpUtil.get(`/hakuperusteetadmin/api/v1/admin?search=${search}`))
      })

    const updateRouteS = Bacon.mergeAll(dispatcher.stream(events.route),Bacon.once(document.location.pathname))
        .map(personOidInUrl)
        .skipDuplicates(_.isEqual)
        .filter(uniquePersonOid => uniquePersonOid ? true : false)
        .flatMap(uniquePersonOid => Bacon.fromPromise(HttpUtil.get(`/hakuperusteetadmin/api/v1/admin/${uniquePersonOid}`)))
        .merge(serverUpdatesBus)
    const viewPaymentChangesRouteS = Bacon.once(document.location.pathname)
      .map(viewDrasticPaymentStatusChangesUrl)
      .skipDuplicates(_.isEqual)
      .filter(isView => isView ? true : false)
      .flatMap(isView => Bacon.fromPromise(HttpUtil.get(`/hakuperusteetadmin/api/v1/admin/users_with_drastic_payment_changes/`)))

    const updateApplicationObjectS = updateRouteS.flatMap(userdata => Bacon.fromArray(userdata.applicationObject ? userdata.applicationObject : []))
    const tarjontaS = updateApplicationObjectS.map(ao => ao.hakukohdeOid).flatMap(fetchFromTarjonta).toEventStream()
    const updateFieldS = dispatcher.stream(events.updateField).merge(serverUpdatesBus)
    const fieldValidationS = dispatcher.stream(events.fieldValidation)
    const updateEducationFormS = dispatcher.stream(events.updateEducationForm)
    const updatePaymentFormS = dispatcher.stream(events.updatePaymentForm)
    const togglePaymentGroupS = dispatcher.stream(events.togglePaymentGroup)

    const stateP = Bacon.update(initialState,
        [propertiesS], onStateInit,
        [searchS], onSearch,
        [fetchUsersFromServerS], onSearchUpdate,
        [tarjontaS], onTarjontaValue,
        [updateRouteS],onUpdateUser,
        [updateEducationFormS], onUpdateEducationForm,
        [togglePaymentGroupS], onTogglePaymentGroup,
        [updateFieldS], onUpdateField,
        [updatePaymentFormS], onUpdatePaymentForm,
        [viewPaymentChangesRouteS], onViewPaymentChanges,
        [fieldValidationS], onFieldValidation)

    const formSubmittedS = stateP.sampledBy(dispatcher.stream(events.submitForm), (state, form) => ({state, form}))
    const userDataFormSubmitS = formSubmittedS.filter(({form}) => form === 'userDataForm').flatMapLatest(({state}) => submitUserDataToServer(state))
    serverUpdatesBus.plug(userDataFormSubmitS)
    userDataFormSubmitS.onValue((_) => hideBusy(document.getElementById('userDataForm')))

    const educationFormSubmitS = formSubmittedS.filter(({form}) => form.match(new RegExp("educationForm_(.*)"))).flatMapLatest(({state, form}) => {
      const hakukohdeOid = form.match(new RegExp("educationForm_(.*)"))[1]
      const applicationObject = _.find(state.applicationObjects, ao => ao.hakukohdeOid === hakukohdeOid)
      return submitEducationDataToServer(state, applicationObject, document.getElementById(form)).map(userdata => {
          return {['form']: form, ['userdata']: userdata}
      })
    });
    serverUpdatesBus.plug(educationFormSubmitS.map(({hakukohdeOid, userdata}) => userdata))
    educationFormSubmitS.onValue(({form}) => hideBusy(document.getElementById(form)))

    const paymentFormSubmitS = formSubmittedS.filter(({form}) => form.match(new RegExp("payment_(.*)"))).flatMapLatest(({state, form}) => {
        const paymentId = form.match(new RegExp("payment_(.*)"))[1]
        const payment = _.find(state.payments, payment => payment.id == paymentId)
        return submitPaymentDataToServer(state, payment, document.getElementById(form)).map(userdata => {
            return {['form']: form, ['userdata']: userdata}
        })
    });
    serverUpdatesBus.plug(paymentFormSubmitS.map(({form, userdata}) => userdata))
    paymentFormSubmitS.onValue(({form}) => hideBusy(document.getElementById(form)))

    function onSearch(state) {
        return {...state, ['isSearching']: true}
    }
    function onStateInit(state, properties) {
        return {...state, properties}
    }
    function onSearchUpdate(state, users) {
        return {...state, ['users']: users, ['isSearching']: false}
    }
    function onTogglePaymentGroup(state, paymentGroup) {
        if(paymentGroup.target.id == 's2016TogglePaymentGroup') {
            return {...state, ['showPaymentGroupS2016']: !state.showPaymentGroupS2016}
        } else {
            return {...state, ['showPaymentGroupK2017']: !state.showPaymentGroupK2017}
        }
    }
    function onUpdateEducationForm(state, newAo) {
        if(newAo == null) {
            return {...state, ['applicationObjects']: []}
        }
        function aoFromServer(id) {
            return _.find(state.fromServer.applicationObject, ao => ao.id == id)
        }
        function decorateWithErrors(a) {
            return applicationObjectWithValidationErrors(_.isMatch(a, aoFromServer(a.id)) ? withNoChanges(a) : withChanges(a))
        }
        function paymentWasNotPreviouslyRequired(id) {
            return !paymentRequiredWithCurrentHakukohdeOid(state, aoFromServer(id))
        }
        function paymentIsCurrentlyRequired(ao) {
            return paymentRequiredWithCurrentHakukohdeOid(state, ao)
        }
        function allPaymentsFailed(payments) {
            return _.every(payments, p => _.contains(["cancel", "error", "unknown"], p.status))
        }
        function updatePaymentRequiredNotification(ao) {
            ao['paymentRequiredNotification'] = (allPaymentsFailed(state.payments)
                                                 && paymentWasNotPreviouslyRequired(ao.id)
                                                 && paymentIsCurrentlyRequired(ao))
            return ao
        }
        function updateAo(oldAo) {
            if (oldAo.id === newAo.id) {
                return updatePaymentRequiredNotification(decorateWithErrors(newAo))
            } else {
                return oldAo
            }
        }
        var updatedAos = _.map(state.applicationObjects, updateAo)
        return {...state, ['applicationObjects']: updatedAos}
    }
    function onTarjontaValue(state, tarjonta) {
        const currentTarjonta = state.tarjonta || []
        const newTarjonta = {...currentTarjonta, [tarjonta.hakukohdeOid]: tarjonta}
        return {...state, ['tarjonta']: newTarjonta}
    }
    function onUpdateField(state, {field, value}) {
        return {...state, [field]: value}
    }
    function onUpdateUser(state, user) {
        const referenceUser = decorateWithIdSelection(user.user)
        const fromServer = {['fromServer']: {...user, ['user']: referenceUser}, ['partialUser']: referenceUser.partialUser}
        const payments = {['payments']: _.map(user.payments, withNoChanges)}
        const applicationObjects = {['applicationObjects']: _.map(user.applicationObject, withNoChanges)}
        const referenceUserWithNoChanges = withNoChanges(referenceUser)
        const hasPaid = {['hasPaid']:user.hasPaid}
        const showPaymentGroupS2016 = {['showPaymentGroupS2016']:false}
        const showPaymentGroupK2017 = {['showPaymentGroupK2017']:false}
        return {...state, ['sessionData']: user.session, ...referenceUserWithNoChanges, ...payments, ...applicationObjects, ...fromServer, ...hasPaid, ...showPaymentGroupS2016, ...showPaymentGroupK2017}
    }
    function onUpdatePaymentForm(state, payment) {
        function decorateWithErrors(pp) {
            const pFromServer = _.find(state.fromServer.payments, p => p.id == payment.id)
            return paymentWithValidationErrors(_.isMatch(pp, pFromServer) ? withNoChanges(pp) : withChanges(pp))
        }
        var updatedPayments = _.map(state.payments, (oldP => oldP.id == payment.id ? decorateWithErrors(payment) : oldP))
        return {...state, ['payments']: updatedPayments}
    }
    function onViewPaymentChanges(state, paymentChanges) {
        return {...state, ['changesView']: paymentChanges}
    }
    function onFieldValidation(state, {field, value}) {
        const newValidationErrors = fieldValidationResults(state)
        return {...state, ['validationErrors']: {...newValidationErrors, ...validateIfNoChanges(state, state.fromServer.user)}}
    }
    function fetchFromTarjonta(hakukohde) {
        return Bacon.fromPromise(HttpUtil.get(tarjontaUrl + "/" + hakukohde))
    }
    function personOidInUrl(url) {
        var match = url.match(new RegExp("oppija/(.*)"))
        return match ? match[1] : null
    }
    function viewDrasticPaymentStatusChangesUrl(url) {
        var match = url.match(new RegExp("d27db1a1-eef3-48f6-84f7-007655c2413f"))
        return match ? true : false
    }
    // Helper functions
    function withPartialPersonId(obj) {
        const HETU_LEN = 11
        return obj.personId.length == HETU_LEN ? {...obj, ['personId']: obj.personId.substring(HETU_LEN - 5, HETU_LEN)} : obj
    }
    function withChanges(obj) {
        const currentValidationErrors = obj.validationErrors || {}
        return {...obj, ['validationErrors']: {...currentValidationErrors, ['noChanges']: null}}
    }
    function withNoChanges(obj) {
        const currentValidationErrors = obj.validationErrors || {}
        return {...obj, ['validationErrors']: {...currentValidationErrors, ['noChanges']: "required"}}
    }
    function decorateWithIdSelection(user) {
        return user.personId ? {...user, ['birthDate']: "", ['idSelection']: ID_PERSONAL_IDENTITY_CODE} : {...user, ['personId']: "", ['idSelection']: ID_BIRTH_DATE}
    }
    function validateIfNoChanges(user, referenceUser) {
        return {['noChanges']: _.isMatch(user, referenceUser) ? "required" : null}
    }

    return stateP
}
