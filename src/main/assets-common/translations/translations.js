import _ from 'lodash'
import Flatten from 'flat'

import * as translations from './translations.json'
const flatTrans = Flatten(translations)

export function translation(key, lang = resolveLang()) {
    const fullKey = key + "." + lang
    const trans = flatTrans[fullKey]
    if (_.isEmpty(trans)) {
        console.log("Missing key " + fullKey)
        return fullKey
    } else {
        return trans
    }
}

export function getTarjontaNameOrFallback(names, lang = resolveLang()){
    return names[lang] || names.fi || names.sv || names.en
}

export function setLang(val) {
    document.cookie="i18next="+val+"; path=/";
}

export function resolveLang() {
    return getCookie("i18next") || resolveLangFromBrowserUrl() || "en"
}

function getCookie(name) {
    var value = "; " + document.cookie;
    var parts = value.split("; " + name + "=");
    if (parts.length == 2) return parts.pop().split(";").shift();
}

function resolveLangFromBrowserUrl() {
    const urlBased = {
        "en": ["studyinfo.fi", "lang=en"],
        "sv": ["studieinfo.fi", "lang=sv"],
        "fi": ["opintopolku.fi", "lang=fi"]
    };
    return Object.keys(urlBased).filter(function (key) {
            return urlBased[key].some(function (url) {
                return location.href.indexOf(url) !== -1
            })
        })[0]
}