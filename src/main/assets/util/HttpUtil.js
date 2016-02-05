import axios from 'axios'
import Promise from 'bluebird'

function getCookie(name) {
  var value = "; " + document.cookie;
  var parts = value.split("; " + name + "=");
  if (parts.length == 2) return parts.pop().split(";").shift();
}

const csrf = getCookie("CSRF")
if(csrf) {
  axios.defaults.headers.common["CSRF"]=csrf;
}
axios.defaults.headers.common["clientSubSystemCode"]="hakuperusteet.web"

export default class HttpUtil {

  static get(url) {
    return HttpUtil.handleResponse(axios.get(url))
  }

  static post(url, jsonData) {
    return HttpUtil.handleResponse(axios.post(url, jsonData))
  }

  static put(url, jsonData) {
    return HttpUtil.handleResponse(axios.put(url, jsonData))
  }

  static handleResponse(httpCall) {
    return new Promise(function(resolve, reject) {
      httpCall
        .then(function(response) {
          resolve(response.data)
        })
        .catch(function(response) {
          reject({
            status: response.status,
            statusText: response.statusText,
            data: response.data
          })
        })
    })
  }
}
