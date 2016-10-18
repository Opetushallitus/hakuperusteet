import {expect, done} from 'chai'
import {commandServer, openPage, hakuperusteetLoaded, testFrame, logout, takeScreenshot, S, S2, wait, select, directLogout} from './testUtil.js'
import {assertDisabled, assertElementsFound, assertEnabled, assertNotFound, assertOneFound, assertSubmitDisabled, assertSubmitEnabled} from './assertions'

function delete_cookie( name ) {
  document.cookie = name + '=; path=/; expires=Thu, 01 Jan 1970 00:00:01 GMT;';
}

delete_cookie("i18next")

describe('Page without session', () => {
  before(directLogout)
  before(commandServer.reset)
  before(openPage("/hakuperusteet", hakuperusteetLoaded))

  it('should show Google login button', assertOneFound(".googleAuthentication.login"))
  it('should not show Google session', assertNotFound(".googleAuthentication.session"))
  it('should show Email login button', assertOneFound(".emailAuthentication.login"))
  it('should not show Email session', assertNotFound(".emailAuthentication.session"))
  it('should not show userDataForm', assertNotFound("#userDataForm"))
  it('should not show educationForm', assertNotFound("#educationForm"))
  it('should not show vetuma start', assertNotFound(".vetumaStart"))
  it('should not show hakuList', assertNotFound(".hakuList"))
})

describe('Page without session - order email token', () => {
  before(openPage("/hakuperusteet/", hakuperusteetLoaded))

  it('submit should be disabled', assertSubmitDisabled())
  it('insert invalid email', setField("#emailToken", "asd@asd.fi asd2@asd.fi"))
  it('submit should be disabled', assertSubmitDisabled())

  it('insert valid email', setField("#emailToken", "asd@asd.fi"))
  it('submit should be enabled', assertSubmitEnabled())

  describe('Submit email token order', () => {
    it('click submit should post emailToken', clickField("#session input[name='submit']"))
    it('should show token order success', assertOneFound("#session .success"))
  })
})

describe('Page without session - invalid login token', () => {
  before(openPage("/hakuperusteet/#/token/nonExistingToken", hakuperusteetLoaded))

  it('should show login error message', assertOneFound(".authentication-error"))
})

describe('Page without session - invalid hakuperiod and maksumuuri not in use', () => {
  before(openPage("/hakuperusteet/ao/1.2.246.562.20.11111111111", hakuperusteetLoaded))
  it('should show maksumuuri error', assertOneFound(".invalidHakuType"))
  it('should show hakuperiod error', assertOneFound(".invalidHakuPeriod"))
  it('should show julkaistu error', assertOneFound(".invalidJulkaistu"))

  it('should not show userDataForm', assertNotFound("#userDataForm"))
  it('should not show educationForm', assertNotFound("#educationForm"))
  it('should not show vetuma start', assertNotFound(".vetumaStart"))
  it('should not show hakuList', assertNotFound(".hakuList"))
})

describe('Page with email session', () => {
  before(commandServer.reset)
  before(openPage("/hakuperusteet/ao/1.2.246.562.20.69046715533/#/token/mochaTestToken", hakuperusteetLoaded))

  describe('Initially userdata', () => {
    it('should show email as loggedIn user', assertOneFound(".loggedInAs"))
    it('should not show email login button', assertNotFound(".emailAuthentication.login"))
    it('should not show Google login button', assertNotFound(".googleAuthentication.login"))
    it('should not show Google session', assertNotFound(".googleAuthentication.session"))
    it('should show logout button', assertOneFound("#logout"))

    it('should show userDataForm', assertOneFound("#userDataForm"))
    it('should not show educationForm', assertNotFound("#educationForm"))
    it('should not show vetuma start', assertNotFound(".vetumaStart"))
    it('should not show hakuList', assertNotFound(".hakuList"))

    it('initially submit should be disabled', assertSubmitDisabled())
    it('initially show all missing errors', assertElementsFound("#userDataForm .error", 7))
  })

  describe('insert firstname', () => {
    before(setField("#firstName", "John"))
    it('submit should be disabled', assertSubmitDisabled())

    describe('insert lastName', () => {
      before(setField("#lastName", "Doe"))
      it('submit should be disabled', assertSubmitDisabled())

      describe('select gender', () => {
        before(clickField("#gender-male"))
        it('submit should be disabled', assertSubmitDisabled())

        describe('select nativeLanguage', () => {
          before(setField("#nativeLanguage", "FI"))
          it('submit should be disabled', assertSubmitDisabled())

          describe('select nationality', () => {
            before(setField("#nationality", "246"))
            it('submit should be disabled', assertSubmitDisabled())
            it('should have person ID field disabled', assertDisabled("#personId"))
            it('should have birthday field disabled', assertDisabled("#birthDate"))

            describe('select personId input', () => {
              before(clickField("#personal-id-yes"))
              it('should have person ID field enabled', assertEnabled("#personId"))
              it('should still have birthday field disabled', assertDisabled("#birthDate"))

              describe('insert personId',  () => {
                before(setField("#personId", "011295-9693"))

                describe('after inserting all userdata',  () => {
                  it('submit should be enabled', assertSubmitEnabled())
                  it('should not show missing errors', assertNotFound("#userDataForm .error"))
                })

                describe('after submitting userDataForm', () => {
                  before(clickField("input[name='submit']"))

                  describe('initially educationdata', () => {
                    it('should show educationForm', assertOneFound("#educationForm"))
                    it('should not show userDataForm', assertNotFound("#userDataForm"))
                    it('should not show vetuma start', assertNotFound(".vetumaStart"))
                    it('should not show hakuList', assertNotFound(".hakuList"))
                    it('submit should be disabled', assertSubmitDisabled())
                    it('show all missing errors', assertElementsFound("#educationForm .error", 2))
                  })

                  describe('select educationLevel Discretionary', () => {
                    before(setField("#educationLevel", "106"))
                    it('submit should be disabled', assertSubmitDisabled())

                    describe('select educationCountry - Solomin Islands', () => {
                      before(setField("#educationCountry", "090"))
                      it('noPaymentRequired should be visible', assertOneFound(".noPaymentRequired"))
                      it('paymentRequired should be hidden', assertNotFound(".paymentRequired"))
                      it('alreadyPaid should be hidden', assertNotFound(".alreadyPaid"))
                      it('should not show missing errors', assertNotFound("#educationForm .error"))
                      it('submit should be enabled', assertSubmitEnabled())
                    })
                  })

                  describe('select educationLevel Bachelor', () => {
                    before(setField("#educationLevel", "116"))

                    describe('select educationCountry - Finland', () => {
                      before(setField("#educationCountry", "246"))
                      it('should not show missing errors', assertNotFound("#educationForm .error"))
                      it('noPaymentRequired should be visible', assertOneFound(".noPaymentRequired"))
                      it('paymentRequired should be hidden', assertNotFound(".paymentRequired"))
                      it('alreadyPaid should be hidden', assertNotFound(".alreadyPaid"))
                      it('submit should be enabled', assertSubmitEnabled())
                    })

                    describe('select educationCountry - Solomin Islands', () => {
                      before(setField("#educationCountry", "090"))

                      describe('before submitting educationdata', () => {
                        it('paymentRequired should be visible', assertOneFound(".paymentRequired"))
                        it('noPaymentRequired should be hidden', assertNotFound(".noPaymentRequired"))
                        it('alreadyPaid should be hidden', assertNotFound(".alreadyPaid"))
                        it('should not show missing errors', assertNotFound("#educationForm .error"))
                        it('submit should be enabled', assertSubmitEnabled())
                      })

                      describe('after submitting educationForm', () => {
                        before(clickField("input[name='submit']"))

                        describe('initially vetuma start page', () => {
                          it('should show vetuma startpage' , assertOneFound(".vetumaStart"))
                          it('should not show userDataForm', assertNotFound("#userDataForm"))
                          it('should not show educationForm', assertNotFound("#educationForm"))
                          it('should not show hakuList', assertNotFound(".hakuList"))

                          // input name=submit is not allowed when doing redirect, hence different name than in other forms
                          it('submit should be enabled', assertEnabled("input[name='submitVetuma']"))
                        })

                        describe('after submitting vetumaForm', () => {
                          before(clickField("input[name='submitVetuma']"))

                          describe('initially hakulist page', () => {
                            it('should show successful payment as result', assertOneFound(".vetumaResult"))
                            it('should not show userDataForm', assertNotFound("#userDataForm"))
                            it('should not show educationForm', assertNotFound("#educationForm"))
                            it('should not show vetuma start', assertNotFound(".vetumaStart"))
                            it('should show hakuList', assertOneFound(".hakuList"))
                            it('submit should be enabled', assertEnabled("input[name='redirectToForm']"))
                          })

                          describe('Submit hakulist form', () => {
                            before(clickField("input[name='redirectToForm']"))
                            describe('after hakulist submit', () => {
                              it('should show mock form', assertOneFound(".mockRedirect"))
                            })

                            describe('Page with email session - add second application object', () => {
                              before(openPage("/hakuperusteet/ao/1.2.246.562.20.31077988074#/token/mochaTestToken", hakuperusteetLoaded))

                              it('should show email as loggedIn user', assertOneFound(".loggedInAs"))
                              it('should not show userDataForm', assertNotFound("#userDataForm"))
                              it('should show educationForm', assertOneFound("#educationForm"))
                              it('should not show vetuma start', assertNotFound(".vetumaStart"))
                              it('should not show hakuList', assertNotFound(".hakuList"))
                              it('initially submit should be disabled', assertSubmitDisabled())
                              it('initially show all missing errors', assertElementsFound("#educationForm .error", 2))

                              describe('select educationLevel 100 and educationCountry Finland', () => {
                                before(() => {
                                  setField("#educationLevel", "100")()
                                  setField("#educationCountry", "246")()
                                })
                                it('submit should be enabled', assertSubmitEnabled())
                                it('should not show missing errors', assertNotFound("#educationForm .error"))
                                it('noPaymentRequired should be visible', assertOneFound(".noPaymentRequired"))
                                it('paymentRequired should be hidden', assertNotFound(".paymentRequired"))
                                it('alreadyPaid should be hidden', assertNotFound(".alreadyPaid"))
                              })

                              describe('select educationLevel 100 and educationCountry Solomin Islands', () => {
                                before(() => {
                                  setField("#educationLevel", "100")()
                                  setField("#educationCountry", "090")()
                                })

                                it('submit should be enabled', assertSubmitEnabled())
                                it('should not show missing errors', assertNotFound("#educationForm .error"))
                                it('noPaymentRequired should be hidden', assertNotFound(".noPaymentRequired"))
                                it('paymentRequired should be hidden', assertNotFound(".paymentRequired"))
                                it('alreadyPaid should be displayed', assertOneFound(".alreadyPaid"))

                                describe('Submit educationForm', () => {
                                  before(clickField("input[name='submit']"))
                                  describe('After adding second application', () => {
                                    it('should show one application object on hakulist page', assertElementsFound(".redirectToForm", 1))
                                  })

                                  describe('Page with email session - no new ao but two existing', () => {
                                    before(openPage("/hakuperusteet/#/token/mochaTestToken", hakuperusteetLoaded))

                                    it('should show email as loggedIn user', assertOneFound(".loggedInAs"))
                                    it('should not show userDataForm', assertNotFound("#userDataForm"))
                                    it('should not show educationForm', assertNotFound("#educationForm"))
                                    it('should not show vetuma start', assertNotFound(".vetumaStart"))
                                    it('should show hakuList', assertOneFound(".hakuList"))
                                  })

                                  describe('Page with email session - new ao for different hakumaksukausi', () => {
                                    before(openPage("/hakuperusteet/ao/1.2.246.562.20.69046715544#/token/mochaTestToken", hakuperusteetLoaded))

                                    it('should show email as loggedIn user', assertOneFound(".loggedInAs"))
                                    it('should not show userDataForm', assertNotFound("#userDataForm"))
                                    it('should show educationForm', assertOneFound("#educationForm"))
                                    it('should not show vetuma start', assertNotFound(".vetumaStart"))
                                    it('should not show hakuList', assertNotFound(".hakuList"))
                                    it('initially submit should be disabled', assertSubmitDisabled())
                                    it('initially show all missing errors', assertElementsFound("#educationForm .error", 2))

                                    describe('select educationLevel 102 and educationCountry Finland', () => {
                                      before(() => {
                                        setField("#educationLevel", "102")()
                                        setField("#educationCountry", "246")()
                                      })

                                      it('submit should be enabled', assertSubmitEnabled())
                                      it('should not show missing errors', assertNotFound("#educationForm .error"))
                                      it('noPaymentRequired should be visible', assertOneFound(".noPaymentRequired"))
                                      it('paymentRequired should be hidden', assertNotFound(".paymentRequired"))
                                      it('alreadyPaid should be hidden', assertNotFound(".alreadyPaid"))
                                    })

                                    describe('select educationLevel 102 and educationCountry Solomin Islands', () => {
                                      before(() => {
                                        setField("#educationLevel", "102")()
                                        setField("#educationCountry", "090")()
                                      })

                                      it('submit should be enabled', assertSubmitEnabled())
                                      it('should not show missing errors', assertNotFound("#educationForm .error"))
                                      it('noPaymentRequired should be hidden', assertNotFound(".noPaymentRequired"))
                                      it('paymentRequired should be displayd', assertOneFound(".paymentRequired"))
                                      it('alreadyPaid should be hidden', assertNotFound(".alreadyPaid"))

                                      describe('Submit educationForm', () => {
                                        before(clickField("input[name='submit']"))
                                        describe('After adding third ao', () => {
                                          it('should show vetuma startpage after submit', assertOneFound(".vetumaStart"))
                                          it('should not show hakuList', assertNotFound(".hakuList"))
                                          it('initially submit should be enabled', assertEnabled("input[name='submitVetuma']"))
                                          it('should show vetuma start', assertOneFound(".vetumaStart"))
                                          it('should not show alreadyPaid', assertNotFound(".alreadyPaid"))
                                        })

                                        describe('Page with email session - no new ao but three existing for two different hakumaksukausi', () => {
                                          before(openPage("/hakuperusteet/#/token/mochaTestToken", hakuperusteetLoaded))
                                          it('should show email as loggedIn user', assertOneFound(".loggedInAs"))
                                          it('should not show userDataForm', assertNotFound("#userDataForm"))
                                          it('should not show educationForm', assertNotFound("#educationForm"))
                                          it('should not show hakuList', assertNotFound(".hakuList"))
                                          it('should show vetuma start', assertOneFound(".vetumaStart"))
                                          it('initially submit should be enabled', assertEnabled("input[name='submitVetuma']"))
                                          it('should not show alreadyPaid', assertNotFound(".alreadyPaid"))

                                          describe('Submit vetumaForm', () => {
                                            it('click submit should go to vetuma and return back with successful payment', clickField("input[name='submitVetuma']"))
                                            it('should show successful payment as result', assertOneFound(".vetumaResult"))

                                            it('should not show userDataForm', assertNotFound("#userDataForm"))
                                            it('should not show educationForm', assertNotFound("#educationForm"))
                                            it('should not show vetuma start', assertNotFound(".vetumaStart"))
                                            it('should show hakuList', assertOneFound(".hakuList"))
                                            it('should show three application objects on hakulist page', assertElementsFound(".redirectToForm", 3))
                                          })
                                        })
                                      })
                                    })
                                  })
                                })
                              })
                            })
                          })
                        })
                      })
                    })
                  })
                })
              })
            })
          })
        })
      })
    })
  })
})

describe('Haku-application landing page', () => {
  before(openPage("/hakuperusteet/app/1.2.3#/token/hakuApp", hakuperusteetLoaded))

  it('should show email as loggedIn user', assertOneFound(".loggedInAs"))
  it('should not show userDataForm', assertNotFound("#userDataForm"))
  it('should not show educationForm', assertNotFound("#educationForm"))
  it('should show vetuma start', assertOneFound(".vetumaStart"))
  it('should not show alreadyPaid', assertNotFound(".alreadyPaid"))

  describe('Submit vetumaForm', () => {
    it('click submit should go to vetuma and return back with successful payment', clickField("input[name='submitVetuma']"))
    it('should show already paid', assertOneFound(".alreadyPaid"))
  })

  describe('Another application - payment exists for hakumaksukausi', () => {
    before(openPage("/hakuperusteet/app/1.2.3.4#/token/hakuApp", hakuperusteetLoaded))
    it('should show email as loggedIn user', assertOneFound(".loggedInAs"))
    it('should not show userDataForm', assertNotFound("#userDataForm"))
    it('should not show educationForm', assertNotFound("#educationForm"))
    it('should not show vetuma start', assertNotFound(".vetumaStart"))
    it('should show alreadyPaid', assertOneFound(".alreadyPaid"))
  })

  describe('Another application - no payment for hakumaksukausi', () => {
    before(openPage("/hakuperusteet/app/1.2.3.5#/token/hakuApp", hakuperusteetLoaded))

    it('should show email as loggedIn user', assertOneFound(".loggedInAs"))
    it('should not show userDataForm', assertNotFound("#userDataForm"))
    it('should not show educationForm', assertNotFound("#educationForm"))
    it('should show vetuma start', assertOneFound(".vetumaStart"))
    it('should not show alreadyPaid', assertNotFound(".alreadyPaid"))

    describe('Submit vetumaForm', () => {
      it('click submit should go to vetuma and return back with successful payment', clickField("input[name='submitVetuma']"))
      it('should show already paid', assertOneFound(".alreadyPaid"))
    })
  })
})

describe('Creating "ulkolomake" with partially generated user', () => {
  before(openPage("/hakuperusteet/ao/1.2.246.562.20.31077988074#/token/hakuApp", hakuperusteetLoaded))

  it('should show userDataForm', assertOneFound("#userDataForm"))

  describe('Insert data', () => {
    it('initially submit should be disabled', assertSubmitDisabled())
    it('initially show all missing errors', assertElementsFound("#userDataForm .error", 7))

    it('insert firstName', setField("#firstName", "Haku"))
    it('submit should be disabled', assertSubmitDisabled())

    it('insert lastName', setField("#lastName", "App"))
    it('submit should be disabled', assertSubmitDisabled())

    it('select personId input', clickField("#personal-id-no"))
    it('should have person ID field disabled', assertDisabled("#personId"))
    it('should have birthday field enabled', assertEnabled("#birthDate"))
    it('insert birthDate', setField("#birthDate", "15051979"))
    it('submit should be disabled', assertSubmitDisabled())

    it('select gender', clickField("#gender-male"))
    it('submit should be disabled', assertSubmitDisabled())

    it('select nativeLanguage', setField("#nativeLanguage", "FI"))
    it('submit should be disabled', assertSubmitDisabled())

    it('select nationality', setField("#nationality", "246"))
    it('submit should be enabled', assertSubmitEnabled())
    it('should not show missing errors', assertNotFound("#userDataForm .error"))
  })

  describe('Submit userDataForm', () => {
    it('click submit should post userdata', clickField("input[name='submit']"))
    it('should open educationForm after submit', assertOneFound("#educationForm"))

    describe('Insert education data', () => {
      it('select educationLevel discretionary', setField("#educationLevel", "100"))
      it('select educationCountry - Solomin Islands', setField("#educationCountry", "090"))
      it('submit should be enabled', assertSubmitEnabled())
      it('should not show missing errors', assertNotFound("#educationForm .error"))
      it('paymentRequired should be visible', assertOneFound(".paymentRequired"))
      it('noPaymentRequired should be hidden', assertNotFound(".noPaymentRequired"))
    })

    describe('Submit educationForm', () => {
      it('click submit should post educationdata', clickField("input[name='submit']"))
      it('should show to application objects on hakulist page', assertElementsFound(".redirectToForm", 1))
    })
  })


})

function setVal(val) { return (e) => { $(e).val(val).focus().blur() }}
function setField(field, val) { return () => { S2(field).then(setVal(val)).then(done).catch(done) }}
function clickField(field) {
  return wait.until(() => {
    const e = select(field)
    if(e.length == 1 && e.attr("disabled") === undefined) {
      e[0].click()
      return true
    } else {
      return false
    }
  })
}
