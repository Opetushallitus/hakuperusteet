import React from 'react'
import Bacon from 'baconjs'
import _ from 'lodash'

import AjaxLoader from '../util/AjaxLoader.jsx'
import {fetchUrlParamsAndRedirectPost} from '../util/FormUtils.js'
import {translation} from '../../assets-common/translations/translations.js'
import {getHakumaksukausiThatRequiresPaymentWhenNoHakukohdeSelected} from '../AppLogic.js'

export default class VetumaStart extends React.Component {

  prepareVetumaUrl(state) {
    const createVetumaUrlPostfix = function () {
      if (!_.isEmpty(state.hakemusOid)) {
        return "/hakemus/" + state.hakemusOid;
      } else if (!_.isEmpty(state.hakukohdeOid)) {
        return "/hakukohde/" + state.hakukohdeOid;
      } else {
        return "/hakumaksukausi/" + getHakumaksukausiThatRequiresPaymentWhenNoHakukohdeSelected(state);
      }
    }

    return state.properties.vetumaStartUrl + createVetumaUrlPostfix()
      + "?href=" + encodeURIComponent(location.href.replace(/ao.*/, "").replace(/app.*/, "").replace("#", ""))
  }

  render() {
    const state = this.props.state
    const vetumaStartUrl = this.prepareVetumaUrl(state)
    return <div className="vetumaStart">
      <div dangerouslySetInnerHTML={{__html: translation("vetuma.start.info")}}/>
      <form id="vetumaStart" onSubmit={fetchUrlParamsAndRedirectPost( vetumaStartUrl)} method="POST">
        <input type="submit" name="submitVetuma" value={translation("vetuma.start.submit")} />
        <AjaxLoader hide={true} />
        <span className="serverError hide">{translation("errors.server.unexpected")}</span>
      </form>
    </div>
  }
}
