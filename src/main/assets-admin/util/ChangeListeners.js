
export function initAdminChangeListeners(dispatcher, events) {
  function pushSearchChange(e) {
    const value = e.target.value
    dispatcher.push(events.search, value)
  }

  function pushRouteChange(path) {
    dispatcher.push(events.route, path)
  }

  function pushPaymentFormChanges(payment, e) {
    const eventObj = valueEventToObject(e)
    dispatcher.push(events.updatePaymentForm, {...payment, ...eventObj})
  }

  function pushEducationFormChanges(ao, e) {
    const eventObj = valueEventToObject(e)
    dispatcher.push(events.updateEducationForm, {...ao, ...eventObj})
  }

  function valueEventToObject(e) {
    return e ? {[e.target.name]: e.target.value} : {}
  }

  return {
    pushPaymentFormChanges: pushPaymentFormChanges,
    pushSearchChange: pushSearchChange,
    pushEducationFormChanges: pushEducationFormChanges,
    pushRouteChange: pushRouteChange
  }
}

