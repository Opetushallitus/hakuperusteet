import React from 'react'
import {translation} from '../assets-common/translations/translations.js'

export default class LogoutPage extends React.Component {
    render() {
        return  <div className="content">
                    <h1>{translation('logoutPage.title')}</h1>
                    <div dangerouslySetInnerHTML={{__html: translation('logoutPage.content')}} />
                </div>
    }
}