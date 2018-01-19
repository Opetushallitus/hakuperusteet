package fi.vm.sade.hakuperusteet.integration.oppijanumerorekisteri

import java.util.Date

import fi.vm.sade.hakuperusteet.DBSupport
import fi.vm.sade.hakuperusteet.domain.{AbstractUser, Google, Henkilo, OppijaToken}
import fi.vm.sade.hakuperusteet.integration.IDP
import fi.vm.sade.hakuperusteet.util.CasClientUtils
import org.http4s.client.{Client, DisposableResponse}
import org.http4s.dsl._
import org.http4s.{Uri, _}
import org.json4s.{DefaultFormats, Formats}
import org.scalatest.{FlatSpec, Matchers}

import scalaz.concurrent.Task

class OnrClientSpec extends FlatSpec with Matchers with DBSupport with CasClientUtils{

  val virkailijaUrl = "https://localhost"
  val virkailijaUri: Uri = Uri(path = virkailijaUrl)

  val idpString: String =
"""[
|{
|"idpEntityId": "google",
|"identifier": "foobar@gmail.com"
|},
|{
|"idpEntityId": "haka",
|"identifier": "foobar@helsinki.fi"
|}
|]""".stripMargin

  val createdJson =
    """{"id": 1,
      |"etunimet": "Testi Foobar",
      |"syntymaaika": null,
      |"kuolinpaiva": null,
      |"hetu":"123456-7890",
      |"kutsumanimi": "Testi",
      |"oidHenkilo": "1.2.3.4",
      |"oppijanumero": null,
      |"sukunimi": "TestiTesti",
      |"sukupuoli": null,
      |"turvakielto": null,
      |"henkiloTyyppi": "OPPIJA",
      |"eiSuomalaistaHetua": false,
      |"passivoitu": false,
      |"yksiloity": false,
      |"yksiloityVTJ": false,
      |"yksilointiYritetty": true,
      |"duplicate": false,
      |"created": 1495203517282,
      |"modified": 1495203540141,
      |"vtjsynced": null,
      |"kasittelijaOid": "1.2.3.4.5.6.7.8.9",
      |"asiointiKieli": null,
      |"aidinkieli": null,
      |"huoltaja": null,
      |"kielisyys": [
      |    {"kieliKoodi": "FI", "kieliTyyppi":"FI"}
      | ],
      |"kansalaisuus": []}""".stripMargin
  val createdOid: String = """1.2.3.4"""

  behavior of "OnrClient"

  it should "save a new user if given empty oid and no idp data" in {
    val nop: Task[Unit] = Task.now[Unit] {}
    implicit val formats: Formats = fi.vm.sade.hakuperusteet.formatsHenkilo
    val mock = Client(
      shutdown = nop,
      open = Service.lift {
        case req@GET -> Root / "oppijanumerorekisteri-service" / "henkilo" / "1.2.3.4" =>
          Ok(createdJson).map(DisposableResponse(_, nop))
        case req@ POST -> Root / "oppijanumerorekisteri-service" / "henkilo" =>
          Ok(createdOid).map(DisposableResponse(_, nop))
        case _ =>
          NotFound().map(DisposableResponse(_, nop))
      }
    )
    val onrClient = new ONRClient(mock)

    val emptyUser = AbstractUser.user(None, None,"", Some(""), Some(""), Some(new Date()), None, OppijaToken, Some(""), Some(""), Some(""), "en")
    val henkilo:Henkilo = onrClient.updateHenkilo(emptyUser)

    henkilo.personOid shouldEqual "1.2.3.4"
  }

  it should "return an user with with a given oid" in {

    val nop: Task[Unit] = Task.now[Unit] {}
    val mock = Client(
      shutdown = nop,
      open = Service.lift {
        case req@GET -> Root / "oppijanumerorekisteri-service" / "henkilo" / "1.2.3.4" =>
          Ok(createdJson).map(DisposableResponse(_, nop))
        case _ =>
          NotFound().map(DisposableResponse(_, nop))
      }
    )
    val onrClient = new ONRClient(mock)

    val user = AbstractUser.user(None, Some("1.2.3.4"),"", Some(""), Some(""), Some(new Date()), None, OppijaToken, Some(""), Some(""), Some(""), "en")
    val henkilo:Henkilo = onrClient.updateHenkilo(user)

    henkilo.personOid shouldEqual "1.2.3.4"
  }

  it should "add idp for given person" in {
    implicit val formats: Formats = fi.vm.sade.hakuperusteet.formatsHenkilo
    val nop: Task[Unit] = Task.now[Unit] {}
    val mock = Client(
      shutdown = nop,
      open = Service.lift {
        case req@POST -> Root / "oppijanumerorekisteri-service" / "henkilo" / "1.2.3.4" / identification =>
          Ok(idpString).map(DisposableResponse(_, nop))
        case _ =>
          NotFound().map(DisposableResponse(_, nop))
      }
    )
    val onrClient = new ONRClient(mock)

    val user = AbstractUser.user(None, Some("1.2.3.4"),"foobar@google.com", Some(""), Some(""), Some(new Date()), None, Google, Some(""), Some(""), Some(""), "en")
    val idptask:Task[List[IDP]] = onrClient.addIdpForHenkilo(user)
    val idp = idptask.unsafePerformSync
    idp.size should be > 0
  }

}
