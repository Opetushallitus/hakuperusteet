package fi.vm.sade.hakuperusteet

import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging
import fi.vm.sade.hakuperusteet.koodisto.{SimplifiedCode, Educations, Languages, Countries}
import fi.vm.sade.hakuperusteet.tarjonta.ApplicationObject
import org.json4s.JValue
import org.json4s.JsonAST.JValue
import org.scalatra.ScalatraServlet
import org.json4s.jackson.JsonMethods._
import org.json4s.JsonDSL._
import org.json4s._
import org.json4s.jackson.Serialization
import org.json4s.jackson.Serialization.{read, write}

class PropertiesServlet(config: Config, countries: Countries, languages: Languages, educations: Educations) extends ScalatraServlet with LazyLogging {
  implicit val formats = Serialization.formats(NoTypeHints)

  before() {
    contentType = "application/json"
  }

  get("/") {
    val properties = Map(
      "userDataUrl" -> "/hakuperusteet/api/v1/session/userData",
      "educationDataUrl" -> "/hakuperusteet/api/v1/session/educationData",
      "logOutUrl" -> "/hakuperusteet/api/v1/session/logout",
      "emailTokenUrl" -> "/hakuperusteet/api/v1/session/emailToken",
      "vetumaStartUrl" -> "/hakuperusteet/api/v1/vetuma/openvetuma",
      "formRedirectUrl" -> "/hakuperusteet/api/v1/form/redirect",
      "countries" -> countries.countries,
      "eeaCountries" -> (countries.eeaCountries ++ List("756")), //todo: fix Switzerland
      "languages" -> languages.languages,
      "baseEducation" -> educations.educations,
      "googleAuthenticationClientId" -> config.getString("google.authentication.client.id"),
      "googleAuthenticationHostedDomain" -> config.getString("google.authentication.hosted.domain")
    )
    write(properties)
  }

  error { case e: Throwable => logger.error("uncaught exception", e) }
}


