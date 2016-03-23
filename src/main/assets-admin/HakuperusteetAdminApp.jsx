import React from 'react'
import Bacon from 'baconjs'
import _ from 'lodash'

import {initAppState, changeListeners} from './AdminState.js'
import HakuperusteetAdminPage from './HakuperusteetAdminPage.jsx'
import urlProperties from './hakuperusteetadmin-web-oph.js'

const appState = initAppState(urlProperties)

appState
    .filter(state => !_.isEmpty(state))
    .onValue((state) => {
        const getReactComponent = function(state) {
            return <HakuperusteetAdminPage controller={changeListeners()} state={state} />
        }
        console.log("Updating UI with state:", state)
        try {
            React.render(getReactComponent(state), document.getElementById('app'))
        } catch (e) {
            console.log('Error from React.render with state', state, e)
        }
    })
