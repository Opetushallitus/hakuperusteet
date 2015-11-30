import _ from 'lodash'

import {ID_BIRTH_DATE, ID_PERSONAL_IDENTITY_CODE} from '../util/Constants'

export function fieldValidationResults(state) {
  return {
    "firstName": validateNonEmptyTextField(state.firstName).concat(validateNameField(state.firstName)),
    "lastName": validateNonEmptyTextField(state.lastName).concat(validateNameField(state.lastName)),
    "birthDate": validateBirthDateDDMMYYYY(state.idSelection, state.birthDate),
    "personId": validatePersonalIdentityCode(state.idSelection, state.personId),
    "gender": validateGender(state.gender),
    "educationCountry": validateSelect(state.educationCountry),
    "educationLevel": validateSelect(state.educationLevel),
    "nationality": validateSelect(state.nationality),
    "nativeLanguage": validateSelect(state.nativeLanguage)
  }
}

export function validateEmailForm(state) {
  return !_.isEmpty(state.emailToken) && _.contains(state.emailToken, "@")
    && !_.contains(state.emailToken, " ") && !_.contains(state.emailToken, ",") && !_.contains(state.emailToken, "\t")
}

export function validateUserDataForm(state) {
  const allV = state.validationErrors || {}
  const userV = [allV.firstName, allV.lastName, allV.birthDate, allV.personId, allV.gender, allV.nativeLanguage,
    allV.nationality].filter(function(x) {return !_.isEmpty(x) })
  return _.all(userV, function(v) { return v.length == 0})
}

export function validateEducationForm(state) {
  const allV = state.validationErrors || {}
  const userV = [allV.educationLevel, allV.educationCountry].filter(function(x) {return !_.isEmpty(x) })
  return _.all(userV, function(v) { return v.length == 0})
}

function validateNonEmptyTextField(value) {
  return (_.isEmpty(value)) ? ["required"] : []
}

function validateNameField(value) {
  const latin1Subset = /^$|^[a-zA-ZÀ-ÖØ-öø-ÿ]$|^[a-zA-ZÀ-ÖØ-öø-ÿ'][a-zA-ZÀ-ÖØ-öø-ÿ ,-.']*(?:[a-zA-ZÀ-ÖØ-öø-ÿ.']+$)$/;
  return latin1Subset.test(value) ? [] : ["invalid"]
}

function validatePersonalIdentityCode(selectedIdField, value) {
  // Simplified personal ID check from range 00 00 00 S 000 0 - 39 19 99 S 999 Y
  //                  d    d    m    m    yy      s     xxx     c
  const pattern = /^([0-3][0-9][0-1][0-9][0-9]{2}[-+Aa][0-9]{3}[0-9A-Ya-y])$/;
  return (selectedIdField == ID_BIRTH_DATE || pattern.test(value)) ? [] : ["required"]
}

function validateBirthDateDDMMYYYY(selectedIdField, value) {
  // Simplified date check from range 00 00 1000 - 39 19 2999
  //                  d    d    m    m    y    yyy
  const pattern = /^([0-3][0-9][0-1][0-9][1-2][0-9]{3})$/;
  if (selectedIdField == ID_PERSONAL_IDENTITY_CODE || pattern.test(value)) {
    return []
  } else {
    return ["invalid"]
  }
}
function validateGender(value) {
  return (_.isEmpty(value)) ? ["required"] : []
}

function validateSelect(value) {
  return (_.isEmpty(value)) ? ["required"] : []
}

export function requiredField(state, fieldName) {
  return !_.isEmpty(state.validationErrors) && !_.isEmpty(state.validationErrors[fieldName])
    && _.contains(state.validationErrors[fieldName], "required")
}

export function invalidField(state, fieldName) {
  return !_.isEmpty(state.validationErrors) && !_.isEmpty(state.validationErrors[fieldName])
    && _.contains(state.validationErrors[fieldName], "invalid")
}
