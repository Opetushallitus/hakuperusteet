import React from 'react'

import {resolveLang, translation} from '../assets-common/translations/translations.js'

export default class Footer extends React.Component {
  render() {
    const lang = resolveLang()
    return <footer>
      <div>
        <div className="footer-logo">
          <a href={translation("footer.oph.link")} title={translation("footer.oph.title")}><img className="oph-logo" alt={translation("footer.oph.title")} src={"/hakuperusteet/img/OPH_logo-" + lang + ".svg"} /></a>
        </div>
        <div className="footer-logo">
          <a href={translation("footer.okm.link")} title={translation("footer.okm.title")}><img alt={translation("footer.okm.title")} src={"/hakuperusteet/img/OKM_logo-" + lang + ".png"} /></a>
        </div>
      </div>
    </footer>
  }
}