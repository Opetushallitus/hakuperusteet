import React from 'react'
import _ from 'lodash'

import './css/props.less'
import '../assets-common/css/hakuperusteet.less'

import {showUserDataForm, showEducationForm, showEducationWithoutPaymentForm, showVetumaStart, showHakuList, isHakuAppView} from './AppLogic.js'
import Header from './Header.jsx'
import Session from './session/Session.jsx'
import ProgramInfo from './programinfo/ProgramInfo.jsx'
import Footer from './Footer.jsx'
import VetumaResultWrapper from './vetuma/VetumaResultWrapper.jsx'
import UserDataForm from './userdata/UserDataForm.jsx'
import EducationForm from './education/EducationForm.jsx'
import EducationWithoutPaymentForm from './education/EducationWithoutPaymentForm.jsx'
import VetumaStart from './vetuma/VetumaStart.jsx'
import HakuList from './hakulist/HakuList.jsx'
import HakuApp from './hakuapp/HakuApp.jsx'
import LogoutPage from './LogoutPage.jsx'

export default class HakuperusteetPage extends React.Component {
  render() {
    const state = this.props.state
    const controller = this.props.controller
    function hakuAppContent() {
      return <div className="content">
        <VetumaResultWrapper state={state}/>
        <HakuApp state={state} controller={controller} />
        <Footer />
      </div>
    }
    function hakuperusteetContent() {
      return <div className="content"><ProgramInfo state={state} />
      <Session state={state} controller={controller} />
      { showUserDataForm(state) ? <UserDataForm state={state} controller={controller} /> : null}
      { showEducationForm(state) ? <EducationForm state={state} controller={controller} /> : null}
      { showEducationWithoutPaymentForm(state) ? <EducationWithoutPaymentForm state={state} controller={controller} /> : null}
      { showVetumaStart(state) ? <VetumaStart state={state} /> : null}
      <VetumaResultWrapper state={state}/>
      { showHakuList(state) ? <HakuList state={state} /> : null}
      <Footer />
      </div>
    }
    const content = state.isLogoutPage && _.isEmpty(state.sessionData)
        ? <LogoutPage />
        : isHakuAppView(state)
          ? hakuAppContent()
          : hakuperusteetContent()

    return <div>
      <Header controller={controller} />
      {content}
    </div>
  }
}
