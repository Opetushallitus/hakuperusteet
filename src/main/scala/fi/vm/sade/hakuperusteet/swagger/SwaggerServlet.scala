package fi.vm.sade.hakuperusteet.swagger

import org.scalatra.ScalatraServlet
import org.scalatra.swagger.{JacksonSwaggerBase, ApiInfo, Swagger}

class AdminSwagger extends Swagger("1.2","1", ApiInfo(
  "Hakuperusteet Admin",
  "Maksumuurin ylläpitokäyttöliittymä",
  "https://opintopolku.fi/wp/fi/opintopolku/tietoa-palvelusta/",
  "verkkotoimitus_opintopolku@oph.fi",
  "EUPL 1.1 or latest approved by the European Commission",
  "http://www.osor.eu/eupl/"
))

class SwaggerServlet(implicit val swagger: Swagger) extends ScalatraServlet with JacksonSwaggerBase