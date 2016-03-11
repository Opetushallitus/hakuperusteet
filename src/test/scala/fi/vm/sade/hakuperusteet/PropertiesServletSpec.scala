package fi.vm.sade.hakuperusteet

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.scalatra.test.scalatest.ScalatraSuite
import org.json4s.jackson.JsonMethods._

@RunWith(classOf[JUnitRunner])
class PropertiesServletSpec extends FunSuite with ScalatraSuite with ServletTestDependencies {
  val s = new PropertiesServlet(config, countries, languages, educations)
  addServlet(s, "/*")

  test("get properties json") {
    get("/") {
      status should equal (200)
      val json = parse(body)
      val ghd = (json \ "googleAuthenticationHostedDomain").extract[Option[String]]
      ghd should equal (Some("https://localhost:18081"))
    }
  }
}
