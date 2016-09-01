package fi.vm.sade.hakuperusteet

import fi.vm.sade.hakuperusteet.validation.{UserValidator, ApplicationObjectValidator}
import org.json4s.jackson.JsonMethods._
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.scalatra.test.scalatest.ScalatraSuite

@RunWith(classOf[JUnitRunner])
class SessionServletSpec extends FunSuite with ScalatraSuite with ServletTestDependencies {
  override val port = 8081

  val hakumaksukausiService = Mockito.mock(classOf[HakumaksukausiService])
  val s = new SessionServlet(config, database, oppijanTunnistus, verifier, UserValidator(countries, languages), ApplicationObjectValidator(countries, educations), emailSender, hakumaksukausiService)
  addServlet(s, "/*")

  val loginPayload = """{"id":1,"email":"firstname.lastname@gmail.com","token":"eyJhbGciOiJSUzI1NiIsImtpZCI6IjNjOTU1NzY1NjdlZTMwNmUzYjg2MmRiMTQ2ZTgxOGM4NjBhMjI4ZWMifQ.eyJpc3MiOiJhY2NvdW50cy5nb29nbGUuY29tIiwiYXRfaGFzaCI6ImxBVUZ4NHN2d21HcHNqOEhIYVpZQ1EiLCJhdWQiOiIzNjA2ODE0ODMwNTYtcWNtcGgwczBicGQ3a29iNTBuMDE5NjFrODFmZW9kMmQuYXBwcy5nb29nbGV1c2VyY29udGVudC5jb20iLCJzdWIiOiIxMDgxNjIwOTg4MDgxMTI1ODAyMTAiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwiYXpwIjoiMzYwNjgxNDgzMDU2LXFjbXBoMHMwYnBkN2tvYjUwbjAxOTYxazgxZmVvZDJkLmFwcHMuZ29vZ2xldXNlcmNvbnRlbnQuY29tIiwiZW1haWwiOiJqdXNzaS52ZXNhbGFAZ21haWwuY29tIiwiaWF0IjoxNDQyOTI3MjM4LCJleHAiOjE0NDI5MzA4MzgsIm5hbWUiOiJKdXNzaSBWZXNhbGEiLCJwaWN0dXJlIjoiaHR0cHM6Ly9saDUuZ29vZ2xldXNlcmNvbnRlbnQuY29tLy1qZ29Qa2ZhRjZVTS9BQUFBQUFBQUFBSS9BQUFBQUFBQUFGWS9SbjAwWS1ndlY3VS9zOTYtYy9waG90by5qcGciLCJnaXZlbl9uYW1lIjoiSnVzc2kiLCJmYW1pbHlfbmFtZSI6IlZlc2FsYSIsImxvY2FsZSI6ImVuLUdCIn0.bz553DiBjLGl2vck11TZ8k8FRgb5FcByBWJdecrqhoTd9cBAZywXjfQhCW4JRRmGrKV-K6CH8OjyoCvCTZU6c7osSxKyEBDuVwwbh-cDDhf-5yOF9qt75F2qBrEP0OvatbKe0CHtWPVqQwdSUHf9ohPRvmzUbGtsEeJnfx6UuMw21ALWhtqoSnG0K1xqO5Pf_5PuRIsV-YpTY3gE7XNS-Dp0HDkq4ojY--sQr6NMC_LkrzBKfz2CwnCo_4_P5VvfkkD13q7O9ZS1XgAtQqotD3y7jhqLXGYEbBqZe9DJfBeWnkXZzh56MvFtTOu-XpvZkuGlD1DxNpYoUWE_U3SRdQ","idpentityid":"google"}"""
  val emailTokenPayload = """{"email":"firstname.lastname@gmail.com","hakukohdeOid":"234234"}"""
  val emailTokenPayloadInvalid1 = """{"email":"firstname.lastname@gmail.com,firstname.lastname@iki.fi"}"""
  val emailTokenPayloadInvalid2 = """{"email":"firstname.lastname@gmail.com firstname.lastname@iki.fi"}"""

  def login = post("/authenticate", loginPayload) { status should equal(200) }

  test("get session") {
    session {
      login
      get("/session") {
        status should equal (200)
        val json = parse(body)
        val email = (json \ "session" \ "email").extract[Option[String]]
        email should equal (Some("firstname.lastname@gmail.com"))
      }
    }
  }

  test("valid emailToken") {
    post("/emailToken", emailTokenPayload) {
      status should equal (200)
      body should equal ("""{"status":"ok"}""")
    }
  }

  test("invalid emailToken") {
    post("/emailToken", emailTokenPayloadInvalid1) {
      status should equal (409)
    }
    post("/emailToken", emailTokenPayloadInvalid2) {
      status should equal (409)
    }
  }
}


