import {expect, done} from 'chai'
import {commandServer, openPage, pageLoaded, S, S2, select, wait, focusAndBlur, click} from './testUtil.js'
import {assertSubmitDisabled, assertSubmitEnabled, assertEnabled, assertDisabled, assertChecked, assertUnchecked, assertValueEmpty, assertValueEqual} from './assertions'

describe('Admin UI front', () => {
  before(commandServer.resetAdmin)
  before(openPage("/hakuperusteetadmin", pageLoaded(form => form.find("#userSearch").length == 1)))
  describe('Search functionality', () => {
    it('insert should be able to filter with email', setField("#userSearch", "anni.annilainen@example.com"))
    it('should show only filtered user', wait.until(() => select(".user").length == 1))
  })
  describe('Modifying user data', () => {
    before(openPage("/hakuperusteetadmin/oppija/1.2.246.562.24.00000001000", pageLoaded(form => form.find("input[value='Annilainen']").length == 1)))

    // Pre-check that identification selection is correctly populated
    it('should have personId option checked', assertChecked("#personal-id-yes"))
    it('should have birthday option unchecked', assertUnchecked("#personal-id-no"))
    it('should have personId field enabled', assertEnabled("#personId"))
    it('should have birthday field disabled', assertDisabled("#birthDate"))
    it('should have personId set', assertValueEqual("#personId", "261095-910P"))
    it('should have birthday empty', assertValueEmpty("#birthDate"))

    it('should change name', setField("#firstName", "Emmi " + getRandomName()))
    it('should change mother tongue', setField("#nativeLanguage", "AB"))
    it('submit should be enabled', assertSubmitEnabled("#userDataForm"))
    it('click submit should post changes', clickField("#userDataForm input[name='submit']"))
    it('submit should be disabled after post', assertSubmitDisabled("#userDataForm"))
  })
  describe('Modifying application object', () => {
    before(openPage("/hakuperusteetadmin/oppija/1.2.246.562.24.00000001001", pageLoaded(form => form.find("input[value='Ossilainen']").length == 1)))
    it('submit should be disabled', assertSubmitDisabled(escape("#educationForm_1.2.246.562.20.69046715533")))
    it('change value of education', setField(escape("#educationCountry_1.2.246.562.20.69046715533"), "016", "010"))
    it('submit should be enabled', assertSubmitEnabled(escape("#educationForm_1.2.246.562.20.69046715533")))
    it('click submit should post changes', clickField(escape("#educationForm_1.2.246.562.20.69046715533") + " input[name='submit']"))
    it('submit should be disabled', assertSubmitDisabled(escape("#educationForm_1.2.246.562.20.69046715533")))
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
      click(e)
      return true
    } else {
      return false
    }
  })
}
