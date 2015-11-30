import {expect, done} from 'chai'
import {S, S2} from './testUtil.js'

export const assertSubmitDisabled = (form = "") => () => S2(form + " input[name='submit']").then(expectToBeDisabled).then(done).catch(done)
export const assertSubmitEnabled = (form = "") => () => S2(form + " input[name='submit']").then(expectToBeEnabled).then(done).catch(done)
export const assertOneFound = (selector) => () => S2(selector).then(expectElementsFound(1)).then(done).catch(done)
export const assertElementsFound = (selector, count) => () => S2(selector).then(expectElementsFound(count)).then(done).catch(done)
export const assertEnabled = (selector) => () => S2(selector).then(expectToBeEnabled).then(done).catch(done)
export const assertChecked = (selector) => () => S2(selector).then(expectToBeChecked).then(done).catch(done)
export const assertUnchecked = (selector) => () => S2(selector).then(expectToBeUnchecked).then(done).catch(done)
export const assertDisabled = (selector) => () => S2(selector).then(expectToBeDisabled).then(done).catch(done)
export const assertNotFound = (selector) => () => { expect(S(selector).length).to.equal(0) }
export const assertValueEqual = (selector, expected) => () => S2(selector).then(e => { expect($(e).val()).to.equal(expected) }).then(done).catch(done)
export const assertValueEmpty = (selector) => () => S2(selector).then(e => { expect($(e).val()).to.equal("") }).then(done).catch(done)

const expectElementsFound = (count) => (e) => { expect(e.length).to.equal(count) }
const expectToBeDisabled = (e) => { expect($(e).attr("disabled")).to.equal("disabled") }
const expectToBeEnabled = (e) => { expect($(e).attr("disabled")).to.equal(undefined) }
const expectToBeChecked = (e) => { expect($(e).attr("checked")).to.equal("checked") }
const expectToBeUnchecked = (e) => { expect($(e).attr("checked")).to.equal(undefined) }
