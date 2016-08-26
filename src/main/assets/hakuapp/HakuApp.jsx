import React from 'react'
import _ from 'lodash'

import AjaxLoader from '../util/AjaxLoader.jsx'

import {translation} from '../../assets-common/translations/translations.js'
import {hasEmailSession, showVetumaStartForHakemus, hasValidPaymentForHakumaksukausi} from '../AppLogic.js'
import {tarjontaForHakukohdeOid} from "../util/TarjontaUtil.js"
import VetumaStart from '../vetuma/VetumaStart.jsx'
import Session from '../session/Session.jsx'
import EmailSession from '../session/EmailSession.jsx'

export default class HakuApp extends React.Component {
  render() {
    const state = this.props.state
    const controller = this.props.controller
    return <section id="haku-app">
      { hasEmailSession(state) ? <EmailSession state={state} controller={controller} /> : null}
      { showVetumaStartForHakemus(state) ? <VetumaStart state={state} /> : null }
      { hasValidPaymentForHakumaksukausi(state) ? <div className="alreadyPaid"><p>{translation("vetuma.result.ok")}</p></div> : null }
    </section>
  }
}