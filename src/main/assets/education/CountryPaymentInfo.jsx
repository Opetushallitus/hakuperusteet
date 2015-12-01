import React from 'react'
import _ from 'lodash'
import {translation} from '../../assets-common/translations/translations.js'

import {hasValidPayment, paymentRequiredWithCurrentHakukohdeOid} from '../AppLogic.js'


export default class CountryPaymentInfo extends React.Component {
  render() {
    const state = this.props.state
    const isDiscretionaryEducationLevel = state.educationLevel === '106'

    const paymentRequired = paymentRequiredWithCurrentHakukohdeOid(state, state)
    const noPaymentRequired = !paymentRequired
    const alreadyPaid = hasValidPayment(state)

    return <div className="userDataFormRow">
      { paymentRequired && !alreadyPaid && !_.isEmpty(state.educationCountry)
        ? <p className="paymentRequired">{translation("apply.payment.required")}</p>
        : null
      }
      { noPaymentRequired && !_.isEmpty(state.educationCountry) && !isDiscretionaryEducationLevel
        ? <p className="noPaymentRequired">{translation("apply.payment.notRequired")}</p>
        : null
      }
      { noPaymentRequired && !_.isEmpty(state.educationCountry) && isDiscretionaryEducationLevel
        ? <p className="noPaymentRequired">{translation("apply.payment.notRequiredHasDiscretionaryEducationLevel")}</p>
        : null
      }
      { alreadyPaid && !noPaymentRequired && !_.isEmpty(state.educationCountry)
        ? <p className="alreadyPaid">{translation("apply.payment.alreadyPaid")}</p>
        : null
      }
    </div>
  }
}
