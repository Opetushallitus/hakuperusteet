
export function initAdminChangeListeners(dispatcher, events) {
  function pushSearchChange(e) {
    const value = e.target.value
    dispatcher.push(events.search, value)
  }

  function pushRouteChange(path) {
    dispatcher.push(events.route, path)
  }

  function pushTogglePaymentGroup(paymentGroup) {
    dispatcher.push(events.togglePaymentGroup, paymentGroup)
  }

  function pushEducationFormChanges(ao, e) {
    const eventObj = valueEventToObject(e)
    dispatcher.push(events.updateEducationForm, {...ao, ...eventObj})
  }

  function valueEventToObject(e) {
    return e ? {[e.target.name]: e.target.value} : {}
  }

  return {
    pushTogglePaymentGroup: pushTogglePaymentGroup,
    pushSearchChange: pushSearchChange,
    pushEducationFormChanges: pushEducationFormChanges,
    pushRouteChange: pushRouteChange
  }
}

