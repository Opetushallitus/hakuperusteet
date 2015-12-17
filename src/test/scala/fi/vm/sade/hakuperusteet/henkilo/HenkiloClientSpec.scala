package fi.vm.sade.hakuperusteet.henkilo

import java.net.URLEncoder
import java.util.Date

import fi.vm.sade.hakuperusteet.domain.{AbstractUser, Henkilo, OppijaToken}
import fi.vm.sade.hakuperusteet.domain.AbstractUser._
import fi.vm.sade.utils.cas.{CasAuthenticatingClient, CasClient, CasParams}
import org.http4s.client.Client
import org.http4s.dsl._
import org.http4s.headers.{Location, `Set-Cookie`}
import org.http4s.{Uri, _}
import org.scalatest.{FlatSpec, Matchers}

import scalaz.concurrent.Task

class HenkiloClientSpec extends FlatSpec with Matchers {
  val virkailijaUrl = "https://localhost"
  val virkailijaUri: Uri = Uri(path = virkailijaUrl)

  behavior of "HenkiloClient"

  it should "send and receive json with CAS headers in place" in {
    val casParams = CasParams("/authentication-service", "foo", "bar")
    val casMock = new CasMock(virkailijaUrl = virkailijaUri, params = casParams)
    val mock = new Client {
      override def shutdown(): Task[Unit] = Task.now[Unit] {}
      override def prepare(req: Request): Task[Response] = req match {
        case req@ POST -> Root / "authentication-service" / "resources" / "s2s" / "hakuperusteet" =>
          casMock.addStep("valid session")
          Ok("""{"personOid":"1.2.3.4","email":"","firstName":"","lastName":"","birthDate":1440742941926,"gender":null,"nationality":"FI","idpentityid":"oppijaToken","educationLevel":""}""")
        case _ =>
          casMock.addStep("invalid request")
          NotFound()
      }
    }
    val client = new CasAuthenticatingClient(new CasClient(virkailijaUri, casMock),
      CasParams("/authentication-service", "foo", "bar"), mock)
    val henkiloClient = new HenkiloClient(virkailijaUrl, client)

    val emptyUser = AbstractUser.user(None, None,"", Some(""), Some(""), Some(new Date()), None, OppijaToken, Some(""), Some(""), Some(""), "en")
    val henkilo:Henkilo = henkiloClient.upsertHenkilo(FindOrCreateUser(emptyUser))

    henkilo.personOid shouldEqual "1.2.3.4"

    casMock.steps should be (List(
      "created TGT-123",
      "created ST-123",
      "created session foobar-123",
      "valid session"
    ))
  }
}

class CasMock(var ticket: String = "123",
              virkailijaUrl: Uri = Uri(path = "https://localhost"),
              params: CasParams) extends Client {
  var steps: List[String] = List()
  def addStep(step: String) = steps = steps :+ step

  override def shutdown(): Task[Unit] = Task.now[Unit] {}

  override def prepare(req: Request): Task[Response] = req match {
    case req@ POST -> Root / "cas" / "v1" / "tickets" => req.decode[String] {
      case body if body == s"username=${params.user.username}&password=${params.user.password}" =>
        addStep(s"created TGT-$ticket")
        Created().withHeaders(Location(Uri(path = s"${virkailijaUrl.toString()}/cas/v1/tickets/TGT-$ticket")))
      case _ =>
        addStep(s"invalid login")
        Unauthorized(Challenge("", ""))
    }

    case req@ POST -> Root / "cas" / "v1" / "tickets" / tgt if tgt == s"TGT-$ticket" => req.decode[String] {
      case service if service == s"service=${URLEncoder.encode(s"${params.service.securityUri.path}", "UTF8")}" =>
        addStep(s"created ST-$ticket")
        Ok(s"ST-$ticket")
      case invurl =>
        addStep("invalid TGT url")
        BadRequest()
    }

    case req@ GET -> Root / service / "j_spring_cas_security_check" if params.service.securityUri.renderString.indexOf(service) > -1 => req.queryString match {
      case s if s == s"ticket=ST-$ticket" =>
        addStep(s"created session foobar-$ticket")
        Ok().withHeaders(`Set-Cookie`(Cookie(name = "JSESSIONID", content = s"foobar-$ticket")))
      case _ =>
        addStep("invalid service ticket")
        Unauthorized(Challenge("", ""))
    }
  }
}
