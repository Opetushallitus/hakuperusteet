package fi.vm.sade.hakuperusteet.henkilo

import java.net.URLEncoder
import java.util.Date

import fi.vm.sade.hakuperusteet.domain.{AbstractUser, Henkilo, OppijaToken}
import fi.vm.sade.hakuperusteet.domain.AbstractUser._
import fi.vm.sade.utils.cas.{CasAuthenticatingClient, CasClient, CasParams}
import org.http4s.client.{Client, DisposableResponse}
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
    val nop: Task[Unit] = Task.now[Unit] {}
    val mock = Client(
      shutdown = nop,
      open = Service.lift {
        case req@ POST -> Root / "authentication-service" / "resources" / "s2s" / "hakuperusteet" =>
          Ok("""{"personOid":"1.2.3.4","email":"","firstName":"","lastName":"","birthDate":1440742941926,"gender":null,"nationality":"FI","idpentityid":"oppijaToken","educationLevel":""}""").map(DisposableResponse(_, nop))
        case _ =>
          NotFound().map(DisposableResponse(_, nop))
      }
    )
    val henkiloClient = new HenkiloClient(mock)

    val emptyUser = AbstractUser.user(None, None,"", Some(""), Some(""), Some(new Date()), None, OppijaToken, Some(""), Some(""), Some(""), "en")
    val henkilo:Henkilo = henkiloClient.upsertHenkilo(FindOrCreateUser(emptyUser))

    henkilo.personOid shouldEqual "1.2.3.4"
  }
}
