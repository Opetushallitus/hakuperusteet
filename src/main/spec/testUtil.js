const waitIntervalMs = 10

window.S = S
window.S2 = S2

$.expr[':'].textContains = function(a, i, m) {
  return $(a).text().indexOf(m[3]) > -1
};

export function S(selector) {
  try {
    if (!testFrame() || !testFrame().jQuery) {
      return $([])
    }
    return $(testFrame().document).find(selector)
  } catch (e) {
    console.log("Premature access to testFrame.jQuery, printing stack trace.")
    console.log(new Error().stack)
    throw e
  }
}

export function S2(selector) {
  var deferred = Q.defer()
  waitUntil(() => findSelector(selector)).then(function() {
    deferred.resolve($(testFrame().document).find(selector))
  })
  return deferred.promise
}

export function findSelector(selector) {
  try {
    return $(testFrame().document).find(selector).length > 0
  } catch(exception){
    return false
  }
}

export function select(selector) {
  try {
    var s = $(testFrame().document).find(selector)
    return s ? s : []
  } catch(exception){
    console.log("Exception when selecting " + selector + ": " + exception)
    return []
  }
}

function findSelectorExactly(selector) {
  try {
    return $(testFrame().document).find(selector).length == 1
  } catch(exception){
    return false
  }
}

export function waitUntil(condition, maxWaitMs) {
  if (maxWaitMs == undefined) maxWaitMs = testTimeoutDefault;
  var deferred = Q.defer()
  var count = maxWaitMs / waitIntervalMs;

  (function waitLoop(remaining) {
    if (condition()) {
      deferred.resolve()
    } else if (remaining === 0) {
      deferred.reject("timeout of " + maxWaitMs + " in wait.until of condition:\n" + condition)
    } else {
      setTimeout(function () {
        waitLoop(remaining - 1)
      }, waitIntervalMs)
    }
  })(count)
  return deferred.promise
}

export function waitUntilFalse(condition, maxWaitMs) {
  return waitUntil(function () {
    return !condition()
  })
}

export function waitforMilliseconds(ms) {
  var deferred = Q.defer()
  setTimeout(function () {
    deferred.resolve()
  }, ms)
  return deferred.promise
}

export function hakuperusteetLoaded() {
  try {
    return testFrame().SESSION_INITED_FOR_TESTING === true
  } catch(exception){
    return false
  }
}

export function pageLoaded(s) {
  return function() {
    try {
      var v = s($(testFrame().document)) //.find(s).length == 1
      waitforMilliseconds(100)
      return v
    } catch (exception) {
      return false
    }
  }
}
export function testFrame() {
  return $("#testframe").get(0).contentWindow
}
export function waitUntilPredicate(predicate) {
  return waitUntil(function () {return predicate() }, testTimeoutPageLoad
  ).then(function () {
      window.uiError = null
      testFrame().onerror = function (err) {
        window.uiError = err;
      } // Hack: force mocha to fail on unhandled exceptions
    })
}
export function openPage(path, predicate) {
  if (!predicate) {
    predicate = function() { return testFrame().jQuery }
  }
  return function () {
    var newTestFrame = $('<iframe>').attr({src: path, width: 1024, height: 800, id: "testframe"}).load(function() {
      var jquery = document.createElement("script")
      jquery.type = "text/javascript"
      jquery.src = "//code.jquery.com/jquery-1.11.1.min.js"
      $(this).contents().find("head")[0].appendChild(jquery)
    })
    $("#testframe").replaceWith(newTestFrame)
    return waitUntilPredicate(predicate)
  }
}

export function logout() {
  return S2("#logout").then((e) => {
    try {
      e.get(0).click()
    } catch(e) {
      // ignore phantomjs logout problem for now
    }
  })
}

function httpGetPromise(url) {
  return function() {
    var deferred = Q.defer()
    $.get(url, (_) => { deferred.resolve() })
    return deferred.promise
  }
}

export const commandServer = {
  reset: httpGetPromise("http://localhost:8000/testoperation/reset"),
  resetAdmin: httpGetPromise("http://localhost:8000/testoperation/resetAdmin")
}

export function takeScreenshot() {
  if (window.callPhantom) {
    var date = new Date()
    var filename = "target/screenshots/" + date.getTime()
    console.log("Taking screenshot " + filename + ".png")
    callPhantom({'screenshot': filename})
  }
}

export var wait = {
  waitIntervalMs: 10,
  until: function(condition, maxWaitMs) {
    return function() {
      if (maxWaitMs == undefined) maxWaitMs = testTimeoutDefault;
      var deferred = Q.defer()
      var count = Math.floor(maxWaitMs / wait.waitIntervalMs);

      (function waitLoop(remaining) {
        if (condition()) {
          deferred.resolve()
        } else if (remaining === 0) {
          const errorStr = "timeout of " + maxWaitMs + "ms in wait.until for condition:\n" + condition
          console.error(new Error(errorStr))
          deferred.reject(errorStr)
        } else {
          setTimeout(function() {
            waitLoop(remaining-1)
          }, wait.waitIntervalMs)
        }
      })(count)
      return deferred.promise
    }
  },
  untilFalse: function(condition) {
    return wait.until(function() { return !condition()})
  },
  forMilliseconds: function(ms) {
    return function() {
      var deferred = Q.defer()
      setTimeout(function() {
        deferred.resolve()
      }, ms)
      return deferred.promise
    }
  }
}

export function directLogout() {
  var deferred = Q.defer()
  $.post("http://localhost:8081/hakuperusteet/api/v1/session/logout", (_) => { deferred.resolve() })
  return deferred.promise
}

export function focusAndBlur(elem) {
  triggerEvent(elem.first(), "focus")
  triggerEvent(elem.first(), "blur")
}

function triggerEvent(element, eventName) {
  const evt = testFrame().document.createEvent('HTMLEvents');
  evt.initEvent(eventName, true, true);
  element[0].dispatchEvent(evt);
}
