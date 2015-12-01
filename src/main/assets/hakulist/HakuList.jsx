import React from 'react'
import Bacon from 'baconjs'
import _ from 'lodash'

import AjaxLoader from '../util/AjaxLoader.jsx'
import {fetchUrlParamsAndRedirectPost} from '../util/FormUtils.js'
import {translation, resolveLang, getTarjontaNameOrFallback} from '../../assets-common/translations/translations.js'

import {tarjontaForHakukohdeOid} from "../util/TarjontaUtil.js"

export default class HakuList extends React.Component {
  render() {
    const state = this.props.state
    return <div className="hakuList">
      <p>{translation("hakulist.info")}</p>

      {[...state.sessionData.applicationObject].map((x, i) =>
        <div key={i}>
          <p>{getTarjontaNameOrFallback(tarjontaForHakukohdeOid(state, x.hakukohdeOid).name)}</p>
          <form className="redirectToForm" onSubmit={fetchUrlParamsAndRedirectPost(state.properties.formRedirectUrl + "?hakukohdeOid=" + x.hakukohdeOid)} method="POST">
            <input type="submit" name="redirectToForm" value={translation("hakulist.submit")} />
            <AjaxLoader hide={true} />
            <span className="serverError hide">{translation("errors.server.unexpected")}</span>
          </form>
        </div>
      )}

      {_.isEmpty(state.sessionData.applicationObject) ? <p>{translation("hakulist.empty")}</p> : null}
      </div>
  }
}
