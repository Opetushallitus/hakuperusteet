package fi.vm.sade.hakuperusteet

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import fi.vm.sade.hakuperusteet.auth.JavaEESessionAuthentication
import fi.vm.sade.hakuperusteet.db.HakuperusteetDatabase
import fi.vm.sade.hakuperusteet.domain.{AbstractUser}
import fi.vm.sade.hakuperusteet.google.GoogleVerifier
import fi.vm.sade.hakuperusteet.oppijantunnistus.OppijanTunnistus
import org.scalatra.ScalatraServlet

class HakuperusteetServlet(val configuration: Config, val db: HakuperusteetDatabase, val oppijanTunnistus: OppijanTunnistus, val googleVerifier: GoogleVerifier) extends ScalatraServlet with JavaEESessionAuthentication with LazyLogging {
  def failUnlessAuthenticated = if (!isAuthenticated) halt(401)

  def userDataFromSession = db.findUser(user.email).getOrElse(halt(500))

  def cookieToLang = cookies.get("i18next").filter(lang => List("en","fi","sv").contains(lang)).getOrElse("en")

  def getUserLang(userData: AbstractUser): String = {
    var lang = userData.uiLang
    if (!List("en","fi","sv").contains(lang)){
      lang = "en"
    }
    lang
  }


  before() {
    contentType = "application/json"
  }

  error { case e: Throwable => logger.error("uncaught exception", e) }
}
