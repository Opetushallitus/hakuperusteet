import Bacon from 'baconjs'

import HttpUtil from '../util/HttpUtil.js'

export function initGoogleAuthentication(properties) {
  return Bacon.fromBinder(sink => {
    gapi.load('auth2', () => {
      var proprs = {
        client_id: properties.googleAuthenticationClientId
        //hosted_domain: properties.googleAuthenticationHostedDomain // todo: enable this
      }
      const auth2 = gapi.auth2.init(proprs)
      auth2.currentUser.listen(currentUser => {
        if (currentUser.isSignedIn()) {
          const email = currentUser.getBasicProfile().getEmail()
          const token = currentUser.getAuthResponse().id_token
          const idpentityid = "google"
          sink({email, token, idpentityid})
        } else {
          sink({})
        }
      })
    })
  })
}

export function logOut(state, controller) {
  return (e) => {
    const promise = Bacon.fromPromise(HttpUtil.post(state.properties.logOutUrl))
    promise.onValue((_) => { controller.logOut() })
    promise.onError((_) => { console.log("logout error") })
    const auth = gapi.auth2.getAuthInstance()
    if (auth.isSignedIn.get()) auth.signOut()
  }
}

export function authenticationClick(_) {
  const auth = gapi.auth2.getAuthInstance()
  if (!auth.isSignedIn.get()) auth.signIn()
}