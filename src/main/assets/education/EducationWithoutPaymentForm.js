import Bacon from 'baconjs'

import HttpUtil from '../util/HttpUtil.js'
import {tarjontaForHakukohdeOid} from "../util/TarjontaUtil.js"

export function submitEducationWithoutPaymentDataToServer(state) {
  const educationData = {
    hakukohdeOid: state.hakukohdeOid,
    hakuOid: tarjontaForHakukohdeOid(state, state.hakukohdeOid).hakuOid,
    educationLevel: "",
    educationCountry: ""
  }
  return Bacon.fromPromise(HttpUtil.post(state.properties.educationDataUrl, educationData))
}
