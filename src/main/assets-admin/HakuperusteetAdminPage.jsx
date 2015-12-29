import React from 'react'
import _ from 'lodash'

import '../assets-common/css/hakuperusteet.less'
import './css/admin.less'
import './css/props.less'

import AdminForm from './HakuperusteetAdminForm.jsx'

export default class HakuperusteetPage extends React.Component {
    constructor(props) {
        super()
        this.changes = props.controller.pushSearchChange
    }

    render() {
        const state = this.props.state
        const controller = this.props.controller
        const users = _.isEmpty(state.users) ? [] : state.users
        const oppijaClassName = state.isSearching ? "sidebar oppija-search searching" : "sidebar oppija-search"
        const fullName = (user) => (user.firstName && user.lastName) ? <span>{user.firstName}&nbsp;{user.lastName}</span> : <span>{user.email}</span>

        const filteredUsers = users.filter(u => {
            if(_.isEmpty(state.userSearch)) {
                return true
            } else {
                var name = (u.firstName + " " + u.lastName).toLowerCase()
                return name.indexOf(state.userSearch.toLowerCase()) > -1
            }
        })
        const results = state.isSearching ? <ul/> : (
            <ul>
                {filteredUsers.map((u, i) => {
                    const selected = u.id == state.id ? "selected user" : "user"
                    return <li key={i} className={selected}><a onClick={this.selectUser.bind(this, u.personOid)}>{fullName(u)}</a></li>;
                    })}
            </ul>
        )
        return <div>
            {state.changesView ?
            <div className="content-area">
                <section className="main-content oppija">
                    <table>
                        <thead>
                        <tr><th>Email</th><th>Linkki käyttäjään</th><th>Maksun tilan muutos</th></tr>
                        </thead>
                        <tbody>
                        {state.changesView.map(row => {

                          return <tr><td>{row.user.email}</td><td><a href="#" onClick={this.selectUser.bind(this, row.user.personOid)}>{row.user.personOid}</a></td><td>{row.old_state ? "Maksettu" :"Kesken"}&rarr;{row.new_state  ? "Maksettu" :"Kesken"}</td></tr>
                          })}
                        </tbody>
                    </table>
                </section>
            </div> : null}
            <div className="content-area">
                <div className={oppijaClassName}>
                    <label htmlFor="userSearch">
                        <span>Opiskelija</span>
                        <input type="text" id="userSearch" name="userSearch" placeholder="Syötä vähintään 3 merkkiä" onChange={this.changes} onBlur={this.changes} maxLength="255" />
                    </label>
                    <div className="user-search">
                        {results}
                    </div>
                    <div className="user-search-summary">
                        Hakutuloksia {filteredUsers.length} kappaletta
                    </div>
                </div>
                <AdminForm state={state} controller={controller} />
            </div>
        </div>
    }

    selectUser(personOid) {
        const controller = this.props.controller
        history.pushState(null, null, `/hakuperusteetadmin/oppija/${personOid}`)
        controller.pushRouteChange(document.location.pathname)
    }
}
