import {expect, done} from 'chai'
import {commandServer, openPage, pageLoaded, S, S2, select, wait, focusAndBlur, click} from './testUtil.js'
import {assertSubmitDisabled, assertSubmitEnabled, assertEnabled, assertDisabled, assertChecked, assertUnchecked, assertValueEmpty, assertValueEqual, assertOneFound, assertNotFound, assertElementsFound} from './assertions'

describe('Admin UI front', () => {
  before(commandServer.resetAdmin)
  before(openPage("/hakuperusteetadmin", pageLoaded(form => form.find("#userSearch").length == 1)))
  describe('Search functionality', () => {
    it('insert should be able to filter with email', setField("#userSearch", "anni.annilainen@example.com"))
    it('should show only filtered user', wait.until(() => select(".user").length == 1))
  })
  describe('Modifying user data', () => {
    before(openPage("/hakuperusteetadmin/oppija/1.2.246.562.24.00000001000", pageLoaded(form => form.find("input[value='Annilainen']").length == 1)))

    describe('Pre-check that identification selection is correctly populated', () => {
      it('should have personId option checked', assertChecked("#personal-id-yes"))
      it('should have birthday option unchecked', assertUnchecked("#personal-id-no"))
      it('should have personId field enabled', assertEnabled("#personId"))
      it('should have birthday field disabled', assertDisabled("#birthDate"))
      it('should have personId set', assertValueEqual("#personId", "261095-910P"))
      it('should have birthday empty', assertValueEmpty("#birthDate"))
    })

    describe('Check that non maksumuuri application has not education for visible', () => {
      it('should have 3 application option', assertElementsFound("label:contains(Hakukohde)", 3))
      it('should have only 2 editable application options', assertElementsFound("[id^=educationForm]", 2))
    })

    describe('Do modifications', () => {
      before(function() {
        setField("#firstName", "Emmi " + getRandomName())()
        setField("#nativeLanguage", "AF")()
      })
      describe('Before submit', () => {
        it('submit should be enabled', assertSubmitEnabled("#userDataForm"))
      })
      describe('After submit', () => {
        before(clickField("#userDataForm input[name='submit']"))
        it('submit should be disabled after post', assertSubmitDisabled("#userDataForm"))
      })
    })
  })
  describe('Modifying application object', () => {
    before(openPage("/hakuperusteetadmin/oppija/1.2.246.562.24.00000001001", pageLoaded(form => form.find("input[value='Ossilainen']").length == 1)))
    it('submit should be disabled', assertSubmitDisabled(escape("#educationForm_1.2.246.562.20.69046715533")))
    it('change value of education', setField(escape("#educationCountry_1.2.246.562.20.69046715533"), "016", "010"))
    it('submit should be enabled', assertSubmitEnabled(escape("#educationForm_1.2.246.562.20.69046715533")))
    it('click submit should post changes', clickField(escape("#educationForm_1.2.246.562.20.69046715533") + " input[name='submit']"))
    it('submit should be disabled', assertSubmitDisabled(escape("#educationForm_1.2.246.562.20.69046715533")))
  })
  describe('No payments', () => {
    before(openPage("/hakuperusteetadmin/oppija/1.2.246.562.24.00000001008", pageLoaded(form => form.find("input[value='Maksuton']").length == 1)))
    it('should have ei maksuja text', assertOneFound("h3:contains('Hakijalla ei ole maksuja')"))
  })
  describe('S2016 payments', () => {
    before(openPage("/hakuperusteetadmin/oppija/1.2.246.562.24.00000001001", pageLoaded(form => form.find("input[value='Ossilainen']").length == 1)))
    it('should have ei maksuja hakumaksukaudelle k2017 text', assertOneFound("h3:contains('Hakijalla ei ole maksuja hakumaksukaudelle k2017')"))
    it('should not have maksut k2017 text', assertNotFound("h3:contains('Maksut hakumaksukaudella k2017')"))
    it('should have maksut s2016 text', assertOneFound("h3:contains('Maksut hakumaksukaudella s2016')"))
    it('should have tila Maksettu', assertOneFound("span:contains('Maksettu')"))
    it('should have maksuloki button', assertOneFound("#s2016TogglePaymentGroup"))
    it('click maksulogi button should open maksuloki', clickField("#s2016TogglePaymentGroup"))
  })
  describe('Payments for both hakumaksukausi', () => {
    before(openPage("/hakuperusteetadmin/oppija/1.2.246.562.24.00000001000", pageLoaded(form => form.find("input[value='Annilainen']").length == 1)))
    it('should have maksut k2017 text', assertOneFound("h3:contains('Maksut hakumaksukaudella k2017')"))
    it('should have maksut s2016 text', assertOneFound("h3:contains('Maksut hakumaksukaudella s2016')"))
    it('should have tila Maksettu', assertOneFound("span:contains('Maksettu')"))
    it('should have tila Kesken', assertOneFound("span:contains('Kesken')"))

    it('should have maksuloki button', assertOneFound("#k2017TogglePaymentGroup"))
    it('click maksulogi button should open maksuloki', clickField("#k2017TogglePaymentGroup"))

    it('should open maksuloki for k2017 hakumaksukausi', assertOneFound("#paymentsGroupk2017[class!=hidden]"))
    it('should not open maksuloki for s2016 hakumaksukausi', assertOneFound("#paymentsGroups2016.hidden"))

    it('change valud of 1st payment status', setField("#paymentsGroupk2017 > form:nth-child(1) > div:nth-child(3) > select[name='status']", "ok"))
    it('click submit for 1st payment should post changes', clickField("#paymentsGroupk2017 > form:nth-child(1) > div:nth-child(4) > div:nth-child(1) > input[type='submit']"))

    it('should have tila Maksettu twice', assertElementsFound("span:contains('Maksettu')", 2))
    it('should not have tila Kesken', assertNotFound("span:contains('Kesken')"))

  })
})

function escape(str) {
  return (str+'').replace(/[.?*+^$[\]\\(){}|-]/g, "\\$&");
};
function getRandomName() {
  var text = "";
  var possible = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

  for( var i=0; i < 7; i++ )
    text += possible.charAt(Math.floor(Math.random() * possible.length));

  return text;
}

function setField(field, val, altval) {
  return wait.until(() => {
    const e = select(field)
    const ok = e.length > 0 ? true : false
    if(ok) {
      if(e.val() == val) {
        val = altval ? altval : val
      }
      e.val(val)
      focusAndBlur(e)
    }
    return ok
})}
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
